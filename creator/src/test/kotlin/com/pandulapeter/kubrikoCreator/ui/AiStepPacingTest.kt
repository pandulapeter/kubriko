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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AiStepPacingTest {
    private fun step(id: Int, isRunning: Boolean = true) = AiStep(id = id, toolName = "add_block", target = CommandTarget.BlockType("Ball"), isRunning = isRunning, changesProject = true)

    @Test
    fun theFirstStepShowsAtOnceAndTheNextOneOnlyAfterTheDelay() = runTest {
        val pacing = AiStepPacing(testTimeSource)
        pacing.show(step(0), DELAY)
        assertEquals(step(0), pacing.shown)
        launch { pacing.show(step(1), DELAY) }
        advanceTimeBy(DELAY - 1L)
        assertEquals(step(0), pacing.shown)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(step(1), pacing.shown)
    }

    @Test
    fun aStepShownLongEnoughIsReplacedAtOnce() = runTest {
        val pacing = AiStepPacing(testTimeSource)
        pacing.show(step(0), DELAY)
        advanceTimeBy(DELAY * 2L)
        pacing.show(step(1), DELAY)
        assertEquals(step(1), pacing.shown)
    }

    @Test
    fun onlyTheNewestOfQuickStepsIsShown() = runTest {
        val pacing = AiStepPacing(testTimeSource)
        pacing.show(step(0), DELAY)
        val first = launch { pacing.show(step(1), DELAY) }
        advanceTimeBy(100)
        first.cancel()
        launch { pacing.show(step(2), DELAY) }
        advanceTimeBy(DELAY - 100L)
        runCurrent()
        assertEquals(step(2), pacing.shown)
    }

    @Test
    fun theStepShownFinishingReplacesItAtOnce() = runTest {
        val pacing = AiStepPacing(testTimeSource)
        pacing.show(step(0), DELAY)
        pacing.show(step(0, isRunning = false), DELAY)
        assertEquals(step(0, isRunning = false), pacing.shown)
    }

    @Test
    fun noDelayShowsEveryStepAtOnce() = runTest {
        val pacing = AiStepPacing(testTimeSource)
        assertNull(pacing.shown)
        pacing.show(step(0), 0)
        pacing.show(step(1), 0)
        assertEquals(step(1), pacing.shown)
    }
}

private const val DELAY = 300
