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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoImpl
import com.pandulapeter.kubriko.helpers.ViewportFrameTickSource
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.Scale
import com.pandulapeter.kubriko.types.TargetFrameRate
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt
import kotlin.time.TimeSource

@Composable
fun InternalViewport(
    modifier: Modifier = Modifier,
    kubriko: Kubriko,
    windowInsets: WindowInsets,
) {
    // Enforce and cache the internal implementation
    val kubrikoImpl = remember(kubriko) {
        kubriko as? KubrikoImpl
            ?: throw IllegalStateException("Custom Kubriko implementations are not supported. Use Kubriko.newInstance() to instantiate Kubriko.")
    }

    // Focus handling
    PlatformFocusEffect { isFocused ->
        kubrikoImpl.stateManager.updateFocus(isFocused)
    }

    // Align the display's actual refresh rate with the game loop's throttle where the platform allows.
    PlatformFrameRateHint(kubrikoImpl.viewportManager.targetFrameRate.collectAsState().value)

    // Publish what the display is capable of, so a game can offer frame rates that suit the panel.
    PlatformMaximumDisplayRefreshRateEffect { kubrikoImpl.metadataManager.updateMaximumDisplayRefreshRateInternal(it) }

    // Engine initialization and viewport-backed frame loop
    LaunchedEffect(Unit) {
        val tickSource = kubrikoImpl.tickSource
        val viewportTickSource = tickSource as? ViewportFrameTickSource
        viewportTickSource?.start()
        // Timestamp of the previous display frame, used to derive the per-frame delta.
        var lastFrameTime = -1L
        // Timestamp of the last emitted tick; the emitted delta is the real time elapsed since it.
        var lastProcessedFrameTime = -1L
        // Subtract-interval accumulator for TargetFrameRate.Limit; preserves the remainder across
        // frames so the achieved rate stays accurate on panels whose refresh rate is not an integer
        // multiple of the target (resetting to zero would drift, e.g. 60 fps on 90 Hz -> 45 fps).
        var phaseInMilliseconds = 0f
        // Display-frame counter for TargetFrameRate.DisplayDivider; emits on every divisor-th frame.
        var displayFramesSinceTick = 0
        // The panel's own frame interval: the gap between the last two display frames awaited back to back,
        // 0 until two such frames have been seen. Frames slept through (see below) are never observed, so only
        // the ones that aren't keep it current.
        var displayFrameInterval = 0f
        // Whether the loop slept through display frames before awaiting the current one, in which case its
        // delta spans several of them and says nothing about the panel's interval.
        var hasSkippedDisplayFrames = false
        // When the previous display frame was processed, on the clock the sleep below is measured against.
        val loopStartTimeMark = TimeSource.Monotonic.markNow()
        var lastFrameProcessedAtInMilliseconds = 0L
        // Hoisted out of the loop: a lambda declared inline in the frame call would capture the mutable locals and
        // be re-allocated on every frame. Nanoseconds rather than withFrameMillis, which wraps whatever it is given
        // in a lambda of its own on every call.
        val onFrame: (Long) -> Unit = { frameTimeInNanoseconds ->
            val frameTimeInMilliseconds = frameTimeInNanoseconds / NANOSECONDS_PER_MILLISECOND
            lastFrameProcessedAtInMilliseconds = loopStartTimeMark.elapsedNow().inWholeMilliseconds
            if (lastFrameTime == -1L) {
                lastFrameTime = frameTimeInMilliseconds
                lastProcessedFrameTime = frameTimeInMilliseconds
                kubrikoImpl.metadataManager.onUpdateInternal(0)
            } else {
                val frameDelta = (frameTimeInMilliseconds - lastFrameTime).toInt()
                lastFrameTime = frameTimeInMilliseconds
                if (!hasSkippedDisplayFrames && frameDelta > 0) {
                    displayFrameInterval = frameDelta.toFloat()
                }
                val canTick = viewportTickSource != null &&
                        viewportTickSource.isRunningInternal.value &&
                        !kubrikoImpl.viewportManager.size.value.isEmpty() &&
                        (!viewportTickSource.shouldPauseOnFocusLoss || kubrikoImpl.stateManager.isFocused.value)
                if (canTick) {
                    when (val targetFrameRate = kubrikoImpl.viewportManager.targetFrameRate.value) {
                        TargetFrameRate.DisplayDefault -> {
                            viewportTickSource.tick((frameTimeInMilliseconds - lastProcessedFrameTime).toInt())
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
                                viewportTickSource.tick((frameTimeInMilliseconds - lastProcessedFrameTime).toInt())
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
                                viewportTickSource.tick((frameTimeInMilliseconds - lastProcessedFrameTime).toInt())
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
        }
        while (isActive) {
            val canTickNow = viewportTickSource != null &&
                    viewportTickSource.isRunningInternal.value &&
                    !kubrikoImpl.viewportManager.size.value.isEmpty() &&
                    (!viewportTickSource.shouldPauseOnFocusLoss || kubrikoImpl.stateManager.isFocused.value)
            if (!canTickNow) {
                if (viewportTickSource == null) {
                    // No viewport-driven ticking is configured; this loop has nothing left to do.
                    awaitCancellation()
                }
                // Suspend on the gate instead of waking at vsync while there is nothing to tick.
                combine(
                    kubrikoImpl.viewportManager.size,
                    kubrikoImpl.stateManager.isFocused,
                    viewportTickSource.isRunningInternal,
                ) { size, isFocused, isTickSourceRunning ->
                    isTickSourceRunning && !size.isEmpty() && (!viewportTickSource.shouldPauseOnFocusLoss || isFocused)
                }.first { it }
                // Resuming: anchor the timeline to now so it doesn't emit one giant catch-up delta.
                lastFrameTime = -1L
                phaseInMilliseconds = 0f
                displayFramesSinceTick = 0
                continue
            }
            // Awaiting a display frame is what schedules one: on every Skia-backed platform the whole window is
            // then drawn again, whether or not a tick changed anything. A throttled target sleeps through the
            // frames that can't carry its next tick instead, and wakes half a display frame before the one that
            // can, so that one is the next frame awaited - the tick decision above still runs on its real time.
            if (lastFrameTime != -1L && displayFrameInterval > 0f) {
                val displayFramesUntilTick = when (val targetFrameRate = kubrikoImpl.viewportManager.targetFrameRate.value) {
                    TargetFrameRate.DisplayDefault -> 1
                    is TargetFrameRate.Limit -> ((1000f / targetFrameRate.framesPerSecond - phaseInMilliseconds) / displayFrameInterval).roundToInt()
                    is TargetFrameRate.DisplayDivider -> targetFrameRate.divisor - displayFramesSinceTick
                }
                if (displayFramesUntilTick >= 2) {
                    val sleepInMilliseconds = ((displayFramesUntilTick - 0.5f) * displayFrameInterval).toLong() -
                            (loopStartTimeMark.elapsedNow().inWholeMilliseconds - lastFrameProcessedAtInMilliseconds)
                    if (sleepInMilliseconds > 0L) {
                        hasSkippedDisplayFrames = true
                        delay(sleepInMilliseconds)
                    }
                }
            }
            withFrameNanos(onFrame)
        }
    }

    // Game canvas
    Box(
        modifier = kubrikoImpl.managers.fold(Modifier.fillMaxSize().clipToBounds()) { overlayModifierToProcess, manager ->
            manager.processOverlayModifierInternal(overlayModifierToProcess)
        }
    ) {
        Box(
            modifier = when (val aspectRatioMode = kubrikoImpl.viewportManager.aspectRatioMode) {
                ViewportManager.AspectRatioMode.Dynamic,
                is ViewportManager.AspectRatioMode.FitHorizontal,
                is ViewportManager.AspectRatioMode.FitVertical,
                is ViewportManager.AspectRatioMode.Stretched -> modifier.fillMaxSize()

                is ViewportManager.AspectRatioMode.Fixed -> modifier
                    .align(aspectRatioMode.alignment)
                    .aspectRatio(ratio = aspectRatioMode.ratio)
            }
                .clipToBounds()
                .onSizeChanged { intSize ->
                    val widthPx = intSize.width.toFloat()
                    val heightPx = intSize.height.toFloat()
                    kubrikoImpl.viewportManager.run {
                        updateSize(Size(widthPx, heightPx))
                        scaleFactorMultiplier.update {
                            when (val mode = aspectRatioMode) {
                                ViewportManager.AspectRatioMode.Dynamic -> Scale.Unit
                                is ViewportManager.AspectRatioMode.FitHorizontal -> (widthPx / mode.width.raw).let { Scale(it, it) }
                                is ViewportManager.AspectRatioMode.FitVertical -> (heightPx / mode.height.raw).let { Scale(it, it) }
                                is ViewportManager.AspectRatioMode.Fixed -> (widthPx / mode.width.raw).let { Scale(it, it) }
                                is ViewportManager.AspectRatioMode.Stretched -> Scale(
                                    horizontal = widthPx / mode.size.width.raw,
                                    vertical = heightPx / mode.size.height.raw,
                                )
                            }
                        }
                    }
                }
        ) {
            // Allow Managers to provide their own Composable functions
            kubrikoImpl.managers.forEach { it.ComposableInternal(windowInsets) }
        }
    }
}

// Compose hands frame times over in nanoseconds; the loop keeps its own time in milliseconds.
private const val NANOSECONDS_PER_MILLISECOND = 1_000_000L
