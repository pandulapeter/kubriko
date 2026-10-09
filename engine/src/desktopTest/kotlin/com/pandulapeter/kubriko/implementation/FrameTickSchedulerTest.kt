/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.implementation

import com.pandulapeter.kubriko.implementation.FrameTickScheduler.Companion.NO_TICK
import com.pandulapeter.kubriko.implementation.FrameTickScheduler.Companion.RE_ANCHOR
import com.pandulapeter.kubriko.types.TargetFrameRate
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FrameTickSchedulerTest {

    private fun frameTime(index: Int, refreshRate: Float) = (index * 1000f / refreshRate).roundToLong()

    private fun FrameTickScheduler.frame(
        frameTimeInMilliseconds: Long,
        targetFrameRate: TargetFrameRate,
        canTick: Boolean = true,
    ) = onFrame(frameTimeInMilliseconds, frameTimeInMilliseconds, canTick, targetFrameRate)

    /** Feeds every display frame of [seconds] at [refreshRate] without sleeping, returning the ticks emitted. */
    private fun countTicks(refreshRate: Float, targetFrameRate: TargetFrameRate, seconds: Int): Int {
        val scheduler = FrameTickScheduler()
        var ticks = 0
        for (i in 0..(refreshRate * seconds).toInt()) {
            if (scheduler.frame(frameTime(i, refreshRate), targetFrameRate) >= 0) ticks++
        }
        return ticks
    }

    /**
     * Simulates the viewport loop, sleeping whenever the scheduler says so and then awaiting the first display frame
     * after the wake-up, returning the frame indices that ticked.
     */
    private fun tickingFramesWithSleeps(
        refreshRate: Float,
        targetFrameRate: TargetFrameRate,
        frameCount: Int,
    ): List<Int> {
        val scheduler = FrameTickScheduler()
        val tickingFrames = ArrayList<Int>()
        var i = 0
        while (i < frameCount) {
            val time = frameTime(i, refreshRate)
            if (scheduler.frame(time, targetFrameRate) >= 0) tickingFrames.add(i)
            val sleep = scheduler.sleepBeforeNextFrame(time, targetFrameRate)
            i++
            if (sleep > 0L) {
                scheduler.onSlept(sleep)
                while (frameTime(i, refreshRate) < time + sleep) i++
            }
        }
        return tickingFrames
    }

    @Test
    fun displayDefaultTicksEveryFrameWithTheFrameDelta() {
        for (refreshRate in listOf(60f, 120f)) {
            val scheduler = FrameTickScheduler()
            assertEquals(RE_ANCHOR, scheduler.frame(frameTime(0, refreshRate), TargetFrameRate.DisplayDefault))
            for (i in 1..200) {
                val delta = frameTime(i, refreshRate) - frameTime(i - 1, refreshRate)
                assertEquals(delta.toInt(), scheduler.frame(frameTime(i, refreshRate), TargetFrameRate.DisplayDefault))
            }
        }
    }

    @Test
    fun limitAtThePanelRateTicksEveryFrameDespiteJitter() {
        val scheduler = FrameTickScheduler()
        var time = 0L
        assertEquals(RE_ANCHOR, scheduler.frame(time, TargetFrameRate.Limit(60)))
        for (i in 1..300) {
            time += if (i % 3 == 0) 16 else 17
            assertTrue(scheduler.frame(time, TargetFrameRate.Limit(60)) > 0, "frame $i did not tick")
        }
    }

    @Test
    fun limitKeepsTheRemainderOnPanelsItDoesNotDivide() {
        assertTrue(abs(countTicks(90f, TargetFrameRate.Limit(60), seconds = 10) - 600) <= 2)
        assertTrue(abs(countTicks(120f, TargetFrameRate.Limit(30), seconds = 10) - 300) <= 2)
    }

    @Test
    fun limitStillReachesItsTargetWhileSleepingBetweenTicks() {
        val tickingFrames = tickingFramesWithSleeps(120f, TargetFrameRate.Limit(30), frameCount = 1200)
        assertTrue(abs(tickingFrames.size - 300) <= 2, "${tickingFrames.size} ticks")
    }

    @Test
    fun displayDividerTicksEverySecondFrame() {
        val scheduler = FrameTickScheduler()
        scheduler.frame(frameTime(0, 120f), TargetFrameRate.DisplayDivider(2))
        for (i in 1..100) {
            val result = scheduler.frame(frameTime(i, 120f), TargetFrameRate.DisplayDivider(2))
            if (i % 2 == 0) assertTrue(result > 0, "frame $i did not tick") else assertEquals(NO_TICK, result)
        }
        assertEquals(
            (2 until 240 step 2).toList(),
            tickingFramesWithSleeps(120f, TargetFrameRate.DisplayDivider(2), frameCount = 240),
        )
    }

    @Test
    fun displayDividerCountsTheFramesASleepSpans() {
        val scheduler = FrameTickScheduler()
        val target = TargetFrameRate.DisplayDivider(2)
        scheduler.frame(frameTime(0, 120f), target)
        scheduler.frame(frameTime(1, 120f), target)
        assertTrue(scheduler.frame(frameTime(2, 120f), target) > 0)
        scheduler.onSlept(20)
        assertEquals(
            (frameTime(5, 120f) - frameTime(2, 120f)).toInt(),
            scheduler.frame(frameTime(5, 120f), target),
        )
    }

    @Test
    fun aLongGapReAnchorsButASleepExtendedOneDoesNot() {
        val scheduler = FrameTickScheduler()
        scheduler.frame(0, TargetFrameRate.DisplayDefault)
        assertEquals(16, scheduler.frame(16, TargetFrameRate.DisplayDefault))
        assertEquals(RE_ANCHOR, scheduler.frame(3_016, TargetFrameRate.DisplayDefault))
        scheduler.onSlept(1_500)
        assertEquals(3_000, scheduler.frame(6_016, TargetFrameRate.DisplayDefault))
    }

    @Test
    fun sleepSpansTheFramesThatCannotCarryTheNextTick() {
        val scheduler = FrameTickScheduler()
        val target = TargetFrameRate.Limit(30)
        for (time in 0L..24L step 8) {
            assertTrue(scheduler.frame(time, target) < 0)
        }
        assertTrue(scheduler.frame(32, target) > 0)
        // 4 frames of 8 ms until the next tick, waking half a frame early, minus 2 ms already spent since the frame.
        assertEquals(26L, scheduler.sleepBeforeNextFrame(34, target))
        assertEquals(0L, scheduler.sleepBeforeNextFrame(34, TargetFrameRate.Limit(125)))
        assertEquals(0L, scheduler.sleepBeforeNextFrame(34, TargetFrameRate.DisplayDefault))
    }

    @Test
    fun catchUpGuardCollapsesTheBacklogAfterALongFrame() {
        val scheduler = FrameTickScheduler()
        val target = TargetFrameRate.Limit(30)
        scheduler.frame(0, target)
        assertEquals(1_000, scheduler.frame(1_000, target))
        for (time in 1_008L..1_024L step 8) {
            assertEquals(NO_TICK, scheduler.frame(time, target))
        }
    }

    @Test
    fun pausingReAnchorsTheTimelineToTheLatestFrame() {
        val scheduler = FrameTickScheduler()
        scheduler.frame(0, TargetFrameRate.DisplayDefault)
        assertEquals(NO_TICK, scheduler.frame(16, TargetFrameRate.DisplayDefault, canTick = false))
        assertEquals(NO_TICK, scheduler.frame(500, TargetFrameRate.DisplayDefault, canTick = false))
        assertEquals(16, scheduler.frame(516, TargetFrameRate.DisplayDefault))
        scheduler.onResumed()
        assertEquals(RE_ANCHOR, scheduler.frame(532, TargetFrameRate.DisplayDefault))
    }
}
