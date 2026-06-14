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
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive

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
        // Hoisted out of the loop: a lambda declared inline in the withFrameMillis call would capture
        // the mutable locals and be re-allocated on every frame.
        val onFrame: (Long) -> Unit = { frameTimeInMilliseconds ->
            if (lastFrameTime == -1L) {
                lastFrameTime = frameTimeInMilliseconds
                lastProcessedFrameTime = frameTimeInMilliseconds
                kubrikoImpl.metadataManager.onUpdateInternal(0)
            } else {
                val frameDelta = (frameTimeInMilliseconds - lastFrameTime).toInt()
                lastFrameTime = frameTimeInMilliseconds
                val canTick = viewportTickSource != null &&
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
                            if (phaseInMilliseconds >= interval) {
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
                            displayFramesSinceTick++
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
        }
        while (isActive) {
            withFrameMillis(onFrame)
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
