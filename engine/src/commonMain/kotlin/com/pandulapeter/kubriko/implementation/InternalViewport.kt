/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.implementation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoImpl
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.Scale
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive

@Composable
fun InternalViewport(
    modifier: Modifier = Modifier,
    kubriko: Kubriko,
    windowInsets: WindowInsets,
) {
    // Enforce and cache the internal implementation
    val kubrikoImpl = remember(kubriko) { kubriko as? KubrikoImpl ?: throw IllegalStateException("Custom Kubriko implementations are not supported. Use Kubriko.newInstance() to instantiate Kubriko.") }

    // Focus handling
    val lifecycleObserver = remember(kubrikoImpl) {
        LifecycleEventObserver { source, _ ->
            kubrikoImpl.stateManager.updateFocus(source.lifecycle.currentState.isAtLeast(activeLifecycleState))
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose { lifecycle.removeObserver(lifecycleObserver) }
    }

    // Inset handling
    val density = LocalDensity.current

    // Game loop
    LaunchedEffect(Unit) {
        kubrikoImpl.initialize()
        var count = 0
        while (isActive) {
            withFrameMillis { frameTimeInMilliseconds ->
                if (kubrikoImpl.metadataManager.totalRuntimeInMilliseconds.value == 0L) {
                    kubrikoImpl.metadataManager.onUpdateInternal(frameTimeInMilliseconds.toInt())
                }
                if (count % Kubriko.frameRate.factor == 0) {
                    (frameTimeInMilliseconds - kubrikoImpl.metadataManager.totalRuntimeInMilliseconds.value).toInt().let { deltaTimeInMilliseconds ->
                        if (!kubrikoImpl.viewportManager.size.value.isEmpty() && kubrikoImpl.stateManager.isFocused.value) {
                            kubrikoImpl.managers.forEach { it.onUpdateInternal(deltaTimeInMilliseconds) }
                        }
                    }
                }
                count++
            }
        }
    }

    // Game canvas
    Box(
        modifier = kubrikoImpl.managers.fold(Modifier.fillMaxSize().clipToBounds()) { overlayModifierToProcess, manager ->
            manager.processOverlayModifierInternal(overlayModifierToProcess)
        }
    ) {
        BoxWithConstraints(
            modifier = when (val aspectRatioMode = kubrikoImpl.viewportManager.aspectRatioMode) {
                ViewportManager.AspectRatioMode.Dynamic,
                is ViewportManager.AspectRatioMode.FitHorizontal,
                is ViewportManager.AspectRatioMode.FitVertical,
                is ViewportManager.AspectRatioMode.Stretched -> modifier
                is ViewportManager.AspectRatioMode.Fixed -> modifier
                    .align(aspectRatioMode.alignment)
                    .aspectRatio(ratio = aspectRatioMode.ratio)
            }.clipToBounds()
        ) {
            LaunchedEffect(maxWidth, maxHeight) {
                with(density) {
                    kubrikoImpl.viewportManager.run {
                        val newSize = Size(maxWidth.toPx(), maxHeight.toPx())
                        updateSize(newSize)
                        scaleFactorMultiplier.update {
                            when (val aspectRatioMode = aspectRatioMode) {
                                ViewportManager.AspectRatioMode.Dynamic -> Scale.Unit
                                is ViewportManager.AspectRatioMode.FitHorizontal -> (maxWidth.toPx() / aspectRatioMode.width.raw).let { Scale(it, it) }
                                is ViewportManager.AspectRatioMode.FitVertical -> (maxHeight.toPx() / aspectRatioMode.height.raw).let { Scale(it, it) }
                                is ViewportManager.AspectRatioMode.Fixed -> (maxWidth.toPx() / aspectRatioMode.width.raw).let { Scale(it, it) }
                                is ViewportManager.AspectRatioMode.Stretched -> Scale(
                                    horizontal = maxWidth.toPx() / aspectRatioMode.size.width.raw,
                                    vertical = maxHeight.toPx() / aspectRatioMode.size.height.raw,
                                )
                            }
                        }
                    }
                }
            }

            // Allow Managers to provide their own Composable functions
            kubrikoImpl.managers.forEach { it.ComposableInternal(windowInsets) }
        }
    }
}