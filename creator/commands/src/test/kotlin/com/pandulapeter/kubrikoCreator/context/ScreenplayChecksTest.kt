/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.commands.screenplay.SetScreenplayReminders
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.screenplay.MentionIndex
import com.pandulapeter.kubrikoCreator.screenplay.ScreenplaySettings
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenplayChecksTest {
    private val subjects = mapOf<RelationSubject, String>(
        RelationSubject.Type("Ball") to ".kubriko/actors/Ball.json",
        RelationSubject.Type("Paddle") to ".kubriko/actors/Paddle.json",
        RelationSubject.Scene("main") to "src/commonMain/composeResources/files/scenes/main.json",
    )
    private val existing = subjects.values.toSet() + "screenplay/README.md"

    @Test
    fun `what no page mentions, what pages mention that is gone, pages off the shape, and built Planned items are named`() {
        val pages = mapOf(
                "README" to "# Breakout\n\nBreak the bricks.\n\nThe [ball](../.kubriko/actors/Ball.json) and the [bat](../.kubriko/actors/Bat.json).\n\n" +
                    "## Planned\n\n- A faster [ball](../.kubriko/actors/Ball.json)\n- A [boss](../.kubriko/actors/Boss.json)\n",
                "notes" to "Only a line, with an ![image](gone.png).",
        )
        val findings = ScreenplayChecks.of(
            pages = pages,
            index = MentionIndex.of(pages),
            subjects = subjects,
            exists = { it in existing },
            areRemindersShown = true,
        )
        assertEquals(
            listOf(
                HealthFinding(HealthFinding.Kind.MENTION_MISSING, RelationSubject.Page("README"), usage = ".kubriko/actors/Bat.json"),
                HealthFinding(HealthFinding.Kind.MENTION_MISSING, RelationSubject.Page("README"), usage = ".kubriko/actors/Boss.json"),
                HealthFinding(HealthFinding.Kind.UNMENTIONED, RelationSubject.Type("Paddle")),
                HealthFinding(HealthFinding.Kind.UNMENTIONED, RelationSubject.Scene("main")),
                HealthFinding(HealthFinding.Kind.PLANNED_MAY_BE_BUILT, RelationSubject.Page("README"), detail = "A faster [ball](../.kubriko/actors/Ball.json)"),
                HealthFinding(HealthFinding.Kind.PAGE_WITHOUT_TITLE, RelationSubject.Page("notes")),
            ),
            findings,
        )
    }

    @Test
    fun `with the reminders off, or no screenplay at all, only mentions of what is gone are named`() {
        val pages = mapOf("README" to "[Bat](../.kubriko/actors/Bat.json)")
        assertEquals(listOf(HealthFinding.Kind.MENTION_MISSING), ScreenplayChecks.of(pages, MentionIndex.of(pages), subjects, { it in existing }, areRemindersShown = false).map { it.kind })
        assertEquals(emptyList(), ScreenplayChecks.of(emptyMap(), MentionIndex(), subjects, { it in existing }, areRemindersShown = true))
    }

    @OptIn(EditorMutation::class)
    @Test
    fun `the reminders are turned off and on with undo, and the open project's checks follow them`() = runTest {
        val context = commandContext(ActorTypeDefinition(name = "Ball"))
        context.screenplay.write("README", "# Game\n\nA game.\n")
        assertEquals(listOf(HealthFinding(HealthFinding.Kind.UNMENTIONED, RelationSubject.Type("Ball"))), context.healthFindings().filter { it.kind == HealthFinding.Kind.UNMENTIONED })
        context.assertRoundTrip(Invocation(SetScreenplayReminders, SetScreenplayReminders.Input(isOn = false))) { ScreenplaySettings.read(context.project.rootDirectory) }
        assertEquals(emptyList(), context.healthFindings().filter { it.kind == HealthFinding.Kind.UNMENTIONED })
    }

    @OptIn(EditorMutation::class)
    @Test
    fun `a file a page links to that is deleted without the page changing is noticed`() = runTest {
        val context = commandContext()
        val sprite = File(context.project.rootDirectory, "src/commonMain/composeResources/drawable/bat.png").apply { parentFile.mkdirs() }.apply { writeText("x") }
        context.screenplay.write("README", "# Game\n\nA game.\n\n![Bat](../src/commonMain/composeResources/drawable/bat.png)\n")
        assertEquals(emptyList(), context.healthFindings().filter { it.kind == HealthFinding.Kind.PAGE_IMAGE_MISSING })
        sprite.delete()
        assertEquals(1, context.healthFindings().count { it.kind == HealthFinding.Kind.PAGE_IMAGE_MISSING })
    }
}
