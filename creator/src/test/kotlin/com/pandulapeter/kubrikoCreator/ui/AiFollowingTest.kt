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
import kotlin.test.assertNull

class AiFollowingTest {
    private fun step(target: CommandTarget?, isRunning: Boolean = false) = AiStep(id = 4, title = Res.string.command_add_block, target = target, isRunning = isRunning)

    @Test
    fun theEditorFollowsStepsAboutActorTypes() {
        assertEquals("Paddle", step(CommandTarget.ActorType("Paddle", 1)).actorTypeToFollow())
        assertNull(step(CommandTarget.Scene("main")).actorTypeToFollow())
        assertNull(step(null).actorTypeToFollow())
    }

    @Test
    fun theOpenActorTypeLightsUpWhatTheStepIsAbout() {
        val finished = step(CommandTarget.ActorType("Paddle", 1, 2))
        assertEquals(ActorTypeHighlight(key = 4, eventIndex = 1, blockIndex = 2), finished.highlightIn("Paddle"))
        assertNull(finished.highlightIn("Ball"))
        assertNull(step(CommandTarget.ActorType("Paddle", 1, 2), isRunning = true).highlightIn("Paddle"))
        assertNull(step(CommandTarget.ActorType("Paddle"), isRunning = true).flashKey)
    }

    @Test
    fun theSidebarLightsUpActorTypesAndScenes() {
        assertEquals("Paddle", step(CommandTarget.ActorType("Paddle")).sidebarName())
        assertEquals("main", step(CommandTarget.Scene("main")).sidebarName())
        assertNull(step(CommandTarget.Game).sidebarName())
    }
}
