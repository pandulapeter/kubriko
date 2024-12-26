package com.pandulapeter.kubriko.implementation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.fold
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.Scale
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive

@Composable
fun InternalViewport(
    modifier: Modifier = Modifier,
    getKubriko: () -> Kubriko,
    windowInsets: WindowInsets,
    overlay: @Composable BoxScope.() -> Unit, // TODO: REMOVE THIS
) {
    // Enforce and cache the internal implementation
    val kubrikoImpl = remember {
        getKubriko() as? KubrikoImpl ?: throw IllegalStateException("Custom Kubriko implementations are not supported. Use Kubriko.newInstance() to instantiate Kubriko.")
    }

    // Focus handling
    val observer = remember { LifecycleEventObserver { source, _ -> kubrikoImpl.stateManager.updateFocus(source.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // Inset handling
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    LaunchedEffect(windowInsets) {
        kubrikoImpl.viewportManager.updateInsetPadding(
            Rect(
                left = windowInsets.getLeft(density, layoutDirection).toFloat(),
                top = windowInsets.getTop(density).toFloat(),
                right = windowInsets.getRight(density, layoutDirection).toFloat(),
                bottom = windowInsets.getBottom(density).toFloat(),
            )
        )
    }

    // Game loop
    initializePlatformSpecificComponents()
    LaunchedEffect(Unit) {
        kubrikoImpl.initialize()
        while (isActive) {
            withFrameNanos { gameTimeInNanos ->
                val previousGameTimeNanos = kubrikoImpl.metadataManager.totalRuntimeInMilliseconds.value * 1000000L
                val deltaTimeInMillis = (gameTimeInNanos - previousGameTimeNanos) / 1000000f
                kubrikoImpl.managers.forEach { it.onUpdateInternal(deltaTimeInMillis, gameTimeInNanos) }
            }
        }
    }

    // Game canvas
    Box(
        modifier = kubrikoImpl.managers.mapNotNull { it.getOverlayModifierInternal() }.toImmutableList().fold().fillMaxSize()
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
            kubrikoImpl.managers.forEach { it.ComposableInternal(Modifier.windowInsetsPadding(windowInsets)) }

            // TODO: Deprecated in favor of Manager Composables
            overlay()
        }
    }
}