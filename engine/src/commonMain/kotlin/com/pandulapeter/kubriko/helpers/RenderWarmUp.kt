/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.helpers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Times a game's per-frame drawing until its cost stops falling, so a loading screen can stay up for exactly as long
 * as the platform still needs to reach full speed.
 *
 * The first seconds of a run cost several times what the same frame settles at: on the JVM the drawing code starts
 * out interpreted and is compiled only once it has run often enough, and every cache it builds on first use is cold
 * on top of that. No fixed number of frames covers that on every device, so the end of it is measured: the code being
 * timed reports what each frame cost it ([measure] or [addWork], then [commitFrame]), and [isWarm] turns true once a
 * rolling average of that has stopped improving.
 *
 * Whatever a plateau would eventually say, a frame cost already inside [imperceptibleFrameWork] ends the wait at
 * once: the point is a game the player cannot see stuttering, not the fastest it will ever get. A cost that never
 * settles still ends it after [maximumDuration], counted from the first frame that drew anything. Measuring stops for
 * good once warm, so the steady state pays nothing for it, and nothing here allocates per frame.
 *
 * Meant to be used from one thread, the one drawing.
 *
 * @param imperceptibleFrameWork A frame cost low enough to end the wait without a plateau. The default is a third of
 *   a 60 fps frame, which leaves room for the cost to grow several times before it costs a frame.
 * @param maximumDuration How long the wait may take at most, from the first frame that drew anything.
 */
class RenderWarmUp internal constructor(
    private val imperceptibleFrameWork: Duration,
    private val maximumDuration: Duration,
    private val clock: TickClock,
) {

    /**
     * Creates a warm-up ending once the frame cost stops falling, falls inside [imperceptibleFrameWork], or after
     * [maximumDuration].
     */
    constructor(
        imperceptibleFrameWork: Duration = DEFAULT_IMPERCEPTIBLE_FRAME_WORK,
        maximumDuration: Duration = DEFAULT_MAXIMUM_DURATION,
    ) : this(imperceptibleFrameWork, maximumDuration, TickClock.Monotonic)

    private val _isWarm = MutableStateFlow(false)

    /** Whether the timed drawing has reached its steady-state cost. Turns true once and stays true. */
    val isWarm: StateFlow<Boolean> = _isWarm.asStateFlow()

    /** Whether the timed drawing should still report its cost. False once [isWarm]. */
    val isMeasuring get() = !_isWarm.value

    private val frameWorkNanoseconds = LongArray(WINDOW_FRAMES)
    private var pendingWorkNanoseconds = 0L
    private var frameCount = 0
    private var firstFrameNanoseconds = 0L
    private var bestAverage = Long.MAX_VALUE
    private var checkpointAverage = Long.MAX_VALUE
    private var checkpointFrame = 0

    /** Runs [work], adding the time it took to the current frame's cost while [isMeasuring]. */
    inline fun <T> measure(work: () -> T): T {
        if (!isMeasuring) return work()
        val start = nowInNanoseconds()
        val result = work()
        addWork(nowInNanoseconds() - start)
        return result
    }

    @PublishedApi
    internal fun nowInNanoseconds() = clock.nowInNanoseconds()

    /** Adds [nanoseconds] of work, timed by the caller, to the current frame's cost while [isMeasuring]. */
    fun addWork(nanoseconds: Long) {
        if (isMeasuring) pendingWorkNanoseconds += nanoseconds
    }

    /**
     * Closes the frame the work added since the last call belongs to. A frame that drew nothing ([hasDrawn] false)
     * is dropped rather than recorded: an empty frame says nothing about how fast the drawing has become.
     */
    fun commitFrame(hasDrawn: Boolean) {
        if (!isMeasuring) return
        val workNanoseconds = pendingWorkNanoseconds
        pendingWorkNanoseconds = 0L
        if (!hasDrawn) return
        // The deadline starts with the first frame that drew anything rather than with construction: a game can wait
        // on its content for far longer than the warm-up itself is allowed to take.
        if (frameCount == 0) firstFrameNanoseconds = clock.nowInNanoseconds()
        frameWorkNanoseconds[frameCount % WINDOW_FRAMES] = workNanoseconds
        frameCount++
        if (clock.nowInNanoseconds() - firstFrameNanoseconds >= maximumDuration.inWholeNanoseconds) {
            _isWarm.value = true
            return
        }
        if (frameCount < WINDOW_FRAMES) return
        var total = 0L
        for (i in frameWorkNanoseconds.indices) total += frameWorkNanoseconds[i]
        val average = total / WINDOW_FRAMES
        if (average < bestAverage) bestAverage = average
        if (bestAverage <= imperceptibleFrameWork.inWholeNanoseconds) {
            _isWarm.value = true
            return
        }
        // Compared across a whole checkpoint rather than between neighbouring frames: the cost falls in many small
        // steps as more of the code is compiled, each of which a per-frame comparison would take for the plateau.
        if (checkpointAverage == Long.MAX_VALUE) {
            checkpointAverage = bestAverage
            checkpointFrame = frameCount
            return
        }
        if (frameCount - checkpointFrame < CHECKPOINT_FRAMES) return
        val hasImproved = bestAverage < checkpointAverage - checkpointAverage / MEANINGFUL_IMPROVEMENT_DIVISOR
        checkpointAverage = bestAverage
        checkpointFrame = frameCount
        if (!hasImproved) _isWarm.value = true
    }

    companion object {
        /** The default frame cost low enough to end the wait at once: a third of a 60 fps frame. */
        val DEFAULT_IMPERCEPTIBLE_FRAME_WORK = 5_500.microseconds

        /** The default longest wait, from the first frame that drew anything. */
        val DEFAULT_MAXIMUM_DURATION = 5.seconds

        internal const val WINDOW_FRAMES = 12
        internal const val CHECKPOINT_FRAMES = 45

        /** Below this share of the last checkpoint's cost the drawing only drifts with the scene. */
        private const val MEANINGFUL_IMPROVEMENT_DIVISOR = 8
    }
}
