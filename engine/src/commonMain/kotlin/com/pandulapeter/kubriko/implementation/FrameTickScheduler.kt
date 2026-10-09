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

import com.pandulapeter.kubriko.types.TargetFrameRate
import kotlin.math.roundToInt

/**
 * Decides, display frame by display frame, when the viewport's frame loop emits a tick and how long it may sleep
 * before awaiting the next frame. Pure: it is fed the frame times, the current time, the tick gate and the target,
 * and keeps no clock of its own.
 */
internal class FrameTickScheduler {
    /** Timestamp of the previous display frame, used to derive the per-frame delta. */
    private var lastFrameTime = -1L

    /** Timestamp of the last emitted tick; the emitted delta is the real time elapsed since it. */
    private var lastProcessedFrameTime = -1L

    /**
     * Subtract-interval accumulator for TargetFrameRate.Limit; preserves the remainder across
     * frames so the achieved rate stays accurate on panels whose refresh rate is not an integer
     * multiple of the target (resetting to zero would drift, e.g. 60 fps on 90 Hz -> 45 fps).
     */
    private var phaseInMilliseconds = 0f

    /** Display-frame counter for TargetFrameRate.DisplayDivider; emits on every divisor-th frame. */
    private var displayFramesSinceTick = 0

    /**
     * The panel's own frame interval: the gap between the last two display frames awaited back to back,
     * 0 until two such frames have been seen. Frames slept through (see below) are never observed, so only
     * the ones that aren't keep it current.
     */
    private var displayFrameInterval = 0f

    /**
     * Whether the loop slept through display frames before awaiting the current one, in which case its
     * delta spans several of them and says nothing about the panel's interval.
     */
    private var hasSkippedDisplayFrames = false

    /**
     * How long the loop slept before awaiting the current frame (0 if it didn't), so that its own throttling
     * is never mistaken for the app having been away.
     */
    private var lastSleepInMilliseconds = 0L

    /** When the previous display frame was processed, on the clock the sleep is measured against. */
    private var lastFrameProcessedAtInMilliseconds = 0L

    /**
     * Processes one display frame. Returns the delta to emit as a tick, [NO_TICK], or [RE_ANCHOR] when the timeline
     * restarted (the first frame, or a long gap), on which the loop reports a zero-length update instead of a tick.
     */
    fun onFrame(
        frameTimeInMilliseconds: Long,
        processedAtInMilliseconds: Long,
        canTick: Boolean,
        targetFrameRate: TargetFrameRate,
    ): Int {
        var result = NO_TICK
        lastFrameProcessedAtInMilliseconds = processedAtInMilliseconds
        // A long gap between frames (the app was in the background) restarts the timeline like the first frame
        // does, instead of being emitted as one giant delta.
        if (lastFrameTime == -1L ||
            frameTimeInMilliseconds - lastFrameTime > MAXIMUM_FRAME_GAP_IN_MILLISECONDS + lastSleepInMilliseconds
        ) {
            lastFrameTime = frameTimeInMilliseconds
            lastProcessedFrameTime = frameTimeInMilliseconds
            phaseInMilliseconds = 0f
            displayFramesSinceTick = 0
            result = RE_ANCHOR
        } else {
            val frameDelta = (frameTimeInMilliseconds - lastFrameTime).toInt()
            lastFrameTime = frameTimeInMilliseconds
            if (!hasSkippedDisplayFrames && frameDelta > 0) {
                displayFrameInterval = frameDelta.toFloat()
            }
            if (canTick) {
                when (targetFrameRate) {
                    TargetFrameRate.DisplayDefault -> {
                        result = (frameTimeInMilliseconds - lastProcessedFrameTime).toInt()
                        lastProcessedFrameTime = frameTimeInMilliseconds
                    }

                    is TargetFrameRate.Limit -> {
                        phaseInMilliseconds += frameDelta
                        val interval = 1000f / targetFrameRate.framesPerSecond
                        // Tick on whichever display frame lands closest to the deadline instead of on the
                        // first one past it: display frames arrive on a grid the target rarely divides, so
                        // demanding a full interval postpones every near-miss by a whole frame and quantizes
                        // the achieved rate down to a fraction of the target - most visibly when the panel
                        // itself runs at the target rate (see PlatformFrameRateHint), where the two throttles
                        // compound instead of stacking. After a sleep the delta spans several display frames,
                        // so the tolerance is half of the panel's own interval rather than of the delta.
                        val displayFrame = if (hasSkippedDisplayFrames) displayFrameInterval else frameDelta.toFloat()
                        if (phaseInMilliseconds >= interval - displayFrame / 2f) {
                            result = (frameTimeInMilliseconds - lastProcessedFrameTime).toInt()
                            lastProcessedFrameTime = frameTimeInMilliseconds
                            phaseInMilliseconds -= interval
                            // Catch-up guard: after a long stall (e.g. backgrounding) collapse the
                            // backlog instead of bursting many ticks to catch up.
                            if (phaseInMilliseconds >= interval) {
                                phaseInMilliseconds = 0f
                            }
                        }
                    }

                    is TargetFrameRate.DisplayDivider -> {
                        displayFramesSinceTick += if (hasSkippedDisplayFrames) {
                            (frameDelta / displayFrameInterval).roundToInt().coerceAtLeast(1)
                        } else {
                            1
                        }
                        if (displayFramesSinceTick >= targetFrameRate.divisor) {
                            result = (frameTimeInMilliseconds - lastProcessedFrameTime).toInt()
                            lastProcessedFrameTime = frameTimeInMilliseconds
                            displayFramesSinceTick = 0
                        }
                    }
                }
            } else {
                // Paused: keep the timeline anchored to the latest frame so resuming does not
                // produce a single giant catch-up delta, and discard accumulated phase.
                lastProcessedFrameTime = frameTimeInMilliseconds
                phaseInMilliseconds = 0f
                displayFramesSinceTick = 0
            }
        }
        hasSkippedDisplayFrames = false
        lastSleepInMilliseconds = 0L
        return result
    }

    /**
     * How long the loop should sleep before awaiting the next display frame, so that a throttled target sleeps through
     * the frames that can't carry its next tick and wakes half a display frame before the one that can. 0 when it
     * should await right away.
     */
    fun sleepBeforeNextFrame(nowInMilliseconds: Long, targetFrameRate: TargetFrameRate): Long {
        if (lastFrameTime != -1L && displayFrameInterval > 0f) {
            val displayFramesUntilTick = when (targetFrameRate) {
                TargetFrameRate.DisplayDefault -> 1
                is TargetFrameRate.Limit -> ((1000f / targetFrameRate.framesPerSecond - phaseInMilliseconds) / displayFrameInterval).roundToInt()
                is TargetFrameRate.DisplayDivider -> targetFrameRate.divisor - displayFramesSinceTick
            }
            if (displayFramesUntilTick >= 2) {
                val sleepInMilliseconds = ((displayFramesUntilTick - 0.5f) * displayFrameInterval).toLong() -
                        (nowInMilliseconds - lastFrameProcessedAtInMilliseconds)
                if (sleepInMilliseconds > 0L) {
                    return sleepInMilliseconds
                }
            }
        }
        return 0L
    }

    /** Records that the loop slept [sleepInMilliseconds] before awaiting the next display frame. */
    fun onSlept(sleepInMilliseconds: Long) {
        hasSkippedDisplayFrames = true
        lastSleepInMilliseconds = sleepInMilliseconds
    }

    /**
     * Resuming after the tick gate was closed: anchors the timeline to the next frame so it doesn't emit one giant
     * catch-up delta.
     */
    fun onResumed() {
        lastFrameTime = -1L
        phaseInMilliseconds = 0f
        displayFramesSinceTick = 0
    }

    companion object {
        /** Returned by [onFrame] when the frame carries no tick. */
        const val NO_TICK = -1

        /** Returned by [onFrame] when the frame restarted the timeline. */
        const val RE_ANCHOR = -2

        private const val MAXIMUM_FRAME_GAP_IN_MILLISECONDS = 2_000L
    }
}
