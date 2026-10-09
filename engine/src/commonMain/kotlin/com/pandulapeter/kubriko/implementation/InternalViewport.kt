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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoImpl
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.helpers.ViewportFrameTickSource
import com.pandulapeter.kubriko.manager.ViewportManager
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlin.time.TimeSource

/**
 * The implementation behind [KubrikoViewport], not meant to be called directly.
 *
 * @param modifier The [Modifier] to be applied to the viewport.
 * @param kubriko The [Kubriko] instance shown by the viewport.
 * @param windowInsets The [WindowInsets] to be used for the viewport.
 */
@Deprecated(
    message = "Use KubrikoViewport.",
    replaceWith = ReplaceWith("KubrikoViewport(modifier, kubriko, windowInsets)", "com.pandulapeter.kubriko.KubrikoViewport"),
)
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

    // A different instance gets a fresh composition: its effects start for it, and the previous one's are disposed.
    key(kubrikoImpl) {
        // One instance can be shown by several viewports at once, so only the last one leaving reports it unfocused.
        DisposableEffect(kubrikoImpl) {
            val stateManager = kubrikoImpl.stateManager
            stateManager.attachedViewportCount++
            onDispose {
                stateManager.attachedViewportCount--
                if (stateManager.attachedViewportCount == 0) {
                    stateManager.updateFocus(false)
                }
            }
        }

        // Focus handling
        PlatformFocusEffect { isFocused ->
            kubrikoImpl.stateManager.updateFocus(isFocused)
        }

        // Align the display's actual refresh rate with the game loop's throttle where the platform allows.
        // Only the empty/non-empty flip of the size recomposes, not every resize.
        val sizeState = kubrikoImpl.viewportManager.size.collectAsState()
        val isSized by remember { derivedStateOf { !sizeState.value.isEmpty() } }
        val targetFrameRate = kubrikoImpl.viewportManager.targetFrameRate.collectAsState().value
        PlatformFrameRateHint(if (isSized) targetFrameRate else null)

        // Publish what the display is capable of, so a game can offer frame rates that suit the panel.
        PlatformMaximumDisplayRefreshRateEffect { kubrikoImpl.metadataManager.updateMaximumDisplayRefreshRateInternal(it) }

        // Engine initialization and viewport-backed frame loop
        LaunchedEffect(Unit) {
            val tickSource = kubrikoImpl.tickSource
            val viewportTickSource = tickSource as? ViewportFrameTickSource
            viewportTickSource?.start()
            val scheduler = FrameTickScheduler()
            val loopStartTimeMark = TimeSource.Monotonic.markNow()
            // Hoisted out of the loop: a lambda declared inline in the frame call would be re-allocated on every frame.
            // Nanoseconds rather than withFrameMillis, which wraps whatever it is given in a lambda of its own on every
            // call.
            val onFrame: (Long) -> Unit = { frameTimeInNanoseconds ->
                val result = scheduler.onFrame(
                    frameTimeInMilliseconds = frameTimeInNanoseconds / NANOSECONDS_PER_MILLISECOND,
                    processedAtInMilliseconds = loopStartTimeMark.elapsedNow().inWholeMilliseconds,
                    canTick = viewportTickSource != null && isTickingAllowed(kubrikoImpl, viewportTickSource),
                    targetFrameRate = kubrikoImpl.viewportManager.targetFrameRate.value,
                )
                when (result) {
                    FrameTickScheduler.RE_ANCHOR -> kubrikoImpl.metadataManager.onUpdateInternal(0)
                    FrameTickScheduler.NO_TICK -> Unit
                    else -> viewportTickSource?.tick(result)
                }
            }
            while (isActive) {
                val canTickNow = viewportTickSource != null && isTickingAllowed(kubrikoImpl, viewportTickSource)
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
                    scheduler.onResumed()
                    continue
                }
                // Awaiting a display frame is what schedules one: on every Skia-backed platform the whole window is
                // then drawn again, whether or not a tick changed anything. A throttled target sleeps through the
                // frames that can't carry its next tick instead, and wakes half a display frame before the one that
                // can, so that one is the next frame awaited - the tick decision still runs on its real time.
                val sleepInMilliseconds = scheduler.sleepBeforeNextFrame(
                    nowInMilliseconds = loopStartTimeMark.elapsedNow().inWholeMilliseconds,
                    targetFrameRate = kubrikoImpl.viewportManager.targetFrameRate.value,
                )
                if (sleepInMilliseconds > 0L) {
                    scheduler.onSlept(sleepInMilliseconds)
                    delay(sleepInMilliseconds)
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
                    .onSizeChanged { intSize -> kubrikoImpl.viewportManager.onViewportSizeChanged(intSize.width.toFloat(), intSize.height.toFloat()) }
            ) {
                // Allow Managers to provide their own Composable functions
                kubrikoImpl.managers.forEach { it.ComposableInternal(windowInsets) }
            }
        }
    }
}

private fun isTickingAllowed(kubrikoImpl: KubrikoImpl, viewportTickSource: ViewportFrameTickSource) =
    viewportTickSource.isRunningInternal.value &&
            !kubrikoImpl.viewportManager.size.value.isEmpty() &&
            (!viewportTickSource.shouldPauseOnFocusLoss || kubrikoImpl.stateManager.isFocused.value)

/** Compose hands frame times over in nanoseconds; the loop keeps its own time in milliseconds. */
private const val NANOSECONDS_PER_MILLISECOND = 1_000_000L
