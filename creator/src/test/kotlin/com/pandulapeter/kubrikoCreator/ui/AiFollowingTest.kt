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

import com.pandulapeter.kubrikoCreator.commands.AiStep
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.ui.actorTypes.ActorTypeHighlight
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection
import kubriko.creator.generated.resources.Res
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiFollowingTest {
    private fun step(target: CommandTarget?, isRunning: Boolean = false, changesProject: Boolean = true) =
        AiStep(id = 4, toolName = "add_block", target = target, isRunning = isRunning, changesProject = changesProject)

    @Test
    fun theEditorFollowsChangesToTypesAndScenes() {
        assertEquals(WorkspaceSelection.BlockType("Paddle"), step(CommandTarget.BlockType("Paddle", 1), isRunning = true).selectionToFollow())
        assertEquals(WorkspaceSelection.Scene("main"), step(CommandTarget.Scene("main")).selectionToFollow())
        assertNull(step(CommandTarget.Game).selectionToFollow())
        assertNull(step(null).selectionToFollow())
    }

    @Test
    fun readsAreNotFollowed() {
        assertNull(step(CommandTarget.BlockType("Paddle"), changesProject = false).selectionToFollow())
        val change = step(CommandTarget.BlockType("Paddle"))
        val read = step(CommandTarget.BlockType("Ball"), changesProject = false).copy(id = 5)
        assertEquals(change, listOf(change, read).newestChange())
        assertNull(listOf(read).newestChange())
    }

    @Test
    fun theOpenActorTypeLightsUpWhatTheStepIsAbout() {
        val finished = step(CommandTarget.BlockType("Paddle", 1, 2))
        assertEquals(ActorTypeHighlight(key = 4, eventIndex = 1, blockIndex = 2), finished.highlightIn("Paddle"))
        assertNull(finished.highlightIn("Ball"))
        assertNull(step(CommandTarget.BlockType("Paddle", 1, 2), isRunning = true).highlightIn("Paddle"))
        assertNull(step(CommandTarget.BlockType("Paddle"), isRunning = true).flashKey)
    }

    @Test
    fun theSidebarLightsUpActorTypesAndScenes() {
        assertEquals("Paddle", step(CommandTarget.BlockType("Paddle")).sidebarName())
        assertEquals("main", step(CommandTarget.Scene("main")).sidebarName())
        assertNull(step(CommandTarget.Game).sidebarName())
    }

    @Test
    fun aStepTheUserWentBackToIsShownUntilTheAiMakesAnotherChange() {
        val latest = step(CommandTarget.BlockType("Paddle"))
        val earlier = step(CommandTarget.Scene("main")).copy(id = 2)
        val revisited = RevisitedAiStep(earlier, latestStepId = 4)
        assertEquals(ShownAiStep(earlier, revisited), shownAiStep(latest, revisited))
        assertEquals(ShownAiStep(latest, flashKey = 4), shownAiStep(latest, null))
        assertEquals(latest, shownAiStep(latest, RevisitedAiStep(earlier, latestStepId = 3))?.step)
        assertNotEquals(shownAiStep(latest, revisited)?.flashKey, shownAiStep(latest, RevisitedAiStep(earlier, latestStepId = 4))?.flashKey)
        assertEquals(ShownAiStep(earlier, revisited), shownAiStep(followed = null, revisited, newestChangeId = 4))
        assertEquals(latest, shownAiStep(latest, revisited, newestChangeId = 6)?.step)
    }

    @Test
    fun onlyTypesAndScenesCanBeGoneTo() {
        assertTrue(step(CommandTarget.BlockType("Paddle")).canBeShown())
        assertTrue(step(CommandTarget.Scene("main")).canBeShown())
        assertFalse(step(CommandTarget.Game).canBeShown())
        assertFalse(step(null).canBeShown())
    }
}
