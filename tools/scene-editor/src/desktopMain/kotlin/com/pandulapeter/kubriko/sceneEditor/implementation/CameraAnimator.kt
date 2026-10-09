/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation

import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.TimeSource

/**
 * Eases the camera of [viewportManager] to a target, yielding to the user as soon as they move the camera themselves.
 */
internal class CameraAnimator(
    private val scope: CoroutineScope,
    private val viewportManager: ViewportManager,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private var cameraAnimationJob: Job? = null

    fun animateCameraTo(target: SceneOffset) {
        val start = viewportManager.cameraPosition.value
        val delta = target - start
        cameraAnimationJob?.cancel()
        cameraAnimationJob = scope.launch {
            val startMark = timeSource.markNow()
            var lastAnimatedPosition = start
            while (isActive) {
                // Any camera movement the animation did not perform means the user took over; yield to them.
                if (viewportManager.cameraPosition.value != lastAnimatedPosition) {
                    return@launch
                }
                val progress = (startMark.elapsedNow().inWholeMilliseconds.toFloat() / CAMERA_ANIMATION_DURATION_MS).coerceIn(0f, 1f)
                lastAnimatedPosition = start + delta * easeInOut(progress)
                viewportManager.setCameraPosition(lastAnimatedPosition)
                if (progress >= 1f) {
                    break
                }
                delay(CAMERA_ANIMATION_FRAME_DELAY_MS)
            }
        }
    }

    fun cancel() {
        cameraAnimationJob?.cancel()
    }

    private companion object {
        const val CAMERA_ANIMATION_DURATION_MS = 350f
        const val CAMERA_ANIMATION_FRAME_DELAY_MS = 8L

        fun easeInOut(progress: Float) = progress * progress * (3f - 2f * progress)
    }
}

internal fun SceneOffset.isRoughlyAt(other: SceneOffset) =
    raw.x.roundToInt() == other.raw.x.roundToInt() && raw.y.roundToInt() == other.raw.y.roundToInt()
