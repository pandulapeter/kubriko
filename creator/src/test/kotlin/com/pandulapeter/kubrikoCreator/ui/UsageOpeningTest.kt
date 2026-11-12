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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.context.RelationKind
import com.pandulapeter.kubrikoCreator.context.RelationPlace
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.Usage
import com.pandulapeter.kubrikoCreator.context.UsageEffect
import com.pandulapeter.kubrikoCreator.context.UsagePlace
import com.pandulapeter.kubrikoCreator.project.CreatorProject
import com.pandulapeter.kubrikoCreator.testing.FakeBlockTypeRepository
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import com.pandulapeter.kubrikoCreator.workspace.RevealedPlace
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceController
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection
import com.pandulapeter.kubrikoCreator.workspace.workspaceController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UsageOpeningTest {
    private val brick = RelationSubject.Type("Brick")

    private fun TestScope.createController(): WorkspaceController {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return workspaceController(
            project = CreatorProject(rootDirectory = testDirectory(), name = "game"),
            parentScope = CoroutineScope(backgroundScope.coroutineContext + dispatcher),
            ioDispatcher = dispatcher,
            scanResources = { _, _ -> emptyMap() },
            blockTypeRepository = FakeBlockTypeRepository(listOf(ActorTypeDefinition(name = "Ball"))),
            openGitRepository = { null },
        ).also { runCurrent() }
    }

    private fun usage(place: UsagePlace) = Usage(place, RelationKind.COLLIDES_WITH, brick, UsageEffect.EVENT_REMOVED)

    @Test
    fun aBlockOfATypeIsShownLitUp() = runTest {
        val selection = createController().selection
        assertTrue(selection.open(usage(UsagePlace.InModel(RelationPlace.Type("Ball", 2, 1)))))
        assertEquals(WorkspaceSelection.BlockType("Ball"), selection.current.value)
        assertEquals(RevealedPlace.InType("Ball", 2, 1), selection.revealed.value?.place)
    }

    @Test
    fun aPageIsShownAndADocumentInThePageEditorToo() = runTest {
        val selection = createController().selection
        assertTrue(selection.open(usage(UsagePlace.Markdown("screenplay/story.md"))))
        assertEquals(WorkspaceSelection.Page("story"), selection.current.value)
        assertTrue(selection.open(usage(UsagePlace.Markdown("NOTES.md"))))
        assertEquals(WorkspaceSelection.Document("NOTES.md"), selection.current.value)
    }

    @Test
    fun whatIsGoneOrHasNoViewIsNotShown() = runTest {
        val selection = createController().selection
        assertFalse(selection.open(usage(UsagePlace.InModel(RelationPlace.Type("Paddle", 0)))), "a type deleted since")
        assertFalse(selection.open(usage(UsagePlace.InModel(RelationPlace.Scene("level")))), "a Scene deleted since")
        assertFalse(selection.open(usage(UsagePlace.KotlinFile("src/Game.kt"))))
        assertFalse(selection.open(usage(UsagePlace.ResourceFile("files/sounds/hit.wav"))))
        assertFalse(selection.open(usage(UsagePlace.GameSettings)))
        assertEquals(WorkspaceSelection.None, selection.current.value)
    }
}
