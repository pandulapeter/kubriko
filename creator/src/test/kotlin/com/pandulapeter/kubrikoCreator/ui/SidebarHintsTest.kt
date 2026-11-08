/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.context.HealthFinding
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_actor_type
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SidebarHintsTest {
    private val unmentionedBall = HealthFinding(HealthFinding.Kind.UNMENTIONED, RelationSubject.Type("Ball"))

    @Test
    fun theScreenplaysFindingsGoToTheRowsTheyAreAboutAndTheOthersDont() {
        val hints = sidebarHintsOf(
            findings = listOf(
                unmentionedBall,
                HealthFinding(HealthFinding.Kind.UNMENTIONED, RelationSubject.Type("Game")),
                HealthFinding(HealthFinding.Kind.UNMENTIONED, RelationSubject.Scene("main")),
                HealthFinding(HealthFinding.Kind.PAGE_WITHOUT_TITLE, RelationSubject.Page("hero")),
                HealthFinding(HealthFinding.Kind.UNREAD_PROPERTY, RelationSubject.Property("Ball", "speed")),
                HealthFinding(HealthFinding.Kind.UNUSED_ACTOR_TYPE, RelationSubject.Type("Ball")),
                HealthFinding(HealthFinding.Kind.UNUSED_PAGE_IMAGE, RelationSubject.Image("screenplay/a.png")),
            ),
            types = listOf(ActorTypeDefinition(name = "Ball"), ManagerTypeDefinition(name = "Game")),
        )
        assertEquals(listOf(unmentionedBall), hints.of(FolderGroup.ACTOR_TYPES, "Ball"))
        assertEquals(1, hints.of(FolderGroup.MANAGER_TYPES, "Game").size)
        assertEquals(1, hints.of(FolderGroup.SCENES, "main").size)
        assertEquals(1, hints.of(FolderGroup.PAGES, "hero").size)
        assertEquals(emptyList(), hints.of(FolderGroup.ACTOR_TYPES, "Game"))
    }

    @Test
    fun aRowWithAHintShowsAQuietMarker() = uiTest(width = 360, height = 80) {
        showing {
            CompositionLocalProvider(LocalSidebarHints provides SidebarHints()) {
                SidebarItem(icon = Res.drawable.ic_actor_type, text = "Ball", onClick = {}, hints = listOf(unmentionedBall))
            }
        }
        onNodeWithText("Ball").assertExists()
        saveSnapshot("sidebar-hint")
    }
}
