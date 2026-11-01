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
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_add_block
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiFollowingTest {
    private fun step(target: CommandTarget?, isRunning: Boolean = false) = AiStep(id = 4, toolName = "add_block", title = Res.string.command_add_block, target = target, isRunning = isRunning)

    @Test
    fun theEditorFollowsStepsAboutActorTypes() {
        assertEquals("Paddle", step(CommandTarget.BlockType("Paddle", 1)).actorTypeToFollow())
        assertNull(step(CommandTarget.Scene("main")).actorTypeToFollow())
        assertNull(step(null).actorTypeToFollow())
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
    fun aStepTheUserWentBackToIsShownUntilTheAiTakesAnother() {
        val latest = step(CommandTarget.BlockType("Paddle"))
        val earlier = step(CommandTarget.Scene("main")).copy(id = 2)
        val revisited = RevisitedAiStep(earlier, latestStepId = 4)
        assertEquals(ShownAiStep(earlier, revisited), shownAiStep(latest, revisited))
        assertEquals(ShownAiStep(latest, flashKey = 4), shownAiStep(latest, null))
        assertEquals(latest, shownAiStep(latest, RevisitedAiStep(earlier, latestStepId = 3))?.step)
        assertNotEquals(shownAiStep(latest, revisited)?.flashKey, shownAiStep(latest, RevisitedAiStep(earlier, latestStepId = 4))?.flashKey)
    }

    @Test
    fun onlyTypesAndScenesCanBeGoneTo() {
        assertTrue(step(CommandTarget.BlockType("Paddle")).canBeShown())
        assertTrue(step(CommandTarget.Scene("main")).canBeShown())
        assertFalse(step(CommandTarget.Game).canBeShown())
        assertFalse(step(null).canBeShown())
    }
}
