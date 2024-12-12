package com.pandulapeter.kubriko.implementation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.fold
import com.pandulapeter.kubriko.implementation.extensions.minus
import com.pandulapeter.kubriko.implementation.extensions.transformForViewport
import com.pandulapeter.kubriko.implementation.extensions.transformViewport
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.types.Scale
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive

@Composable
fun InternalViewport(
    modifier: Modifier = Modifier,
    getKubriko: () -> Kubriko,
    overlay: @Composable BoxScope.() -> Unit,
) {
    // Enforce and cache the internal implementation
    val kubrikoImpl = remember {
        getKubriko() as? KubrikoImpl ?: throw IllegalStateException("Custom Kubriko implementations are not supported. Use Kubriko.newInstance() to instantiate Kubriko.")
    }

    // Focus handling
    val observer = remember { LifecycleEventObserver { source, _ -> kubrikoImpl.stateManager.updateFocus(source.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) { lifecycle.addObserver(observer) }
    DisposableEffect(lifecycle) { onDispose { lifecycle.removeObserver(observer) } }

    // Game loop
    val gameTime = remember { mutableStateOf(0L) }
    initializePlatformSpecificComponents()
    LaunchedEffect(Unit) {
        kubrikoImpl.initialize()
        while (isActive) {
            withFrameNanos { gameTimeInNanos ->
                val deltaTimeInMillis = if (gameTime.value == 0L) 0f else (gameTimeInNanos - gameTime.value) / 1000000f
                kubrikoImpl.managers.forEach { it.onUpdateInternal(deltaTimeInMillis, gameTimeInNanos) }
                gameTime.value = gameTimeInNanos
            }
        }
    }

    // Game canvas
    Box(
        modifier = Modifier.fillMaxSize(),
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
            val density = LocalDensity.current
            LaunchedEffect(maxWidth, maxHeight) {
                with(density) {
                    kubrikoImpl.viewportManager.run {
                        val newSize = Size(maxWidth.toPx(), maxHeight.toPx())
                        updateSize(newSize)
                        scaleFactorMultiplier.update {
                            when (val aspectRatioMode = aspectRatioMode) {
                                ViewportManager.AspectRatioMode.Dynamic -> Scale.Unit
                                is ViewportManager.AspectRatioMode.FitHorizontal -> (maxWidth.toPx() / aspectRatioMode.defaultWidth.raw).let { Scale(it, it) }
                                is ViewportManager.AspectRatioMode.FitVertical -> (maxHeight.toPx() / aspectRatioMode.defaultHeight.raw).let { Scale(it, it) }
                                is ViewportManager.AspectRatioMode.Fixed -> (maxWidth.toPx() / aspectRatioMode.defaultWidth.raw).let { Scale(it, it) }
                                is ViewportManager.AspectRatioMode.Stretched -> Scale(
                                    horizontal = maxWidth.toPx() / aspectRatioMode.defaultWidth.raw,
                                    vertical = maxHeight.toPx() / aspectRatioMode.defaultHeight.raw,
                                )
                            }
                        }
                    }
                }
            }
            LayerCluster(
                gameTime = gameTime.value,
                getKubriko = { kubrikoImpl },
            )
            overlay()
        }
    }
}

@Composable
private fun LayerCluster(
    gameTime: Long,
    getKubriko: () -> KubrikoImpl,
) = getKubriko().let { kubrikoImpl ->
    Box(
        modifier = kubrikoImpl.managers.mapNotNull { it.getModifierInternal(null) }.toImmutableList().fold()
    ) {
        kubrikoImpl.actorManager.layerIndices.value.forEach { layerIndex ->
            Layer(
                gameTime = gameTime,
                getKubriko = getKubriko,
                layerIndex = layerIndex,
            )
        }
    }
}

@Composable
private fun Layer(
    gameTime: Long,
    getKubriko: () -> KubrikoImpl,
    layerIndex: Int?,
) = getKubriko().let { kubrikoImpl ->
    kubrikoImpl.viewportManager.cameraPosition.value.let { viewportCenter ->
        Canvas(
            modifier = if (layerIndex == null) {
                Modifier.fillMaxSize().clipToBounds()
            } else {
                kubrikoImpl.managers.mapNotNull { it.getModifierInternal(layerIndex) }.toImmutableList().fold()
            },
            onDraw = {
                @Suppress("UNUSED_EXPRESSION") gameTime  // This line invalidates the Canvas, causing a refresh on every frame
                withTransform(
                    transformBlock = {
                        transformViewport(
                            viewportCenter = viewportCenter,
                            shiftedViewportOffset = (kubrikoImpl.viewportManager.size.value / 2f) - viewportCenter,
                            viewportScaleFactor = kubrikoImpl.viewportManager.scaleFactor.value,
                        )
                    },
                    drawBlock = {
                        kubrikoImpl.actorManager.visibleActorsWithinViewport.value
                            .filter { it.isVisible && it.layerIndex == layerIndex }
                            .forEach { visible ->
                                withTransform(
                                    transformBlock = { visible.transformForViewport(this) },
                                    drawBlock = {
                                        with(visible) {
                                            clipRect(
                                                left = -CLIPPING_BORDER,
                                                top = -CLIPPING_BORDER,
                                                right = body.size.width.raw + CLIPPING_BORDER,
                                                bottom = body.size.height.raw + CLIPPING_BORDER,
                                            ) {
                                                draw()
                                            }
                                        }
                                    }
                                )
                            }
                    }
                )
                kubrikoImpl.actorManager.overlayActors.value
                    .filter { it.layerIndex == layerIndex }
                    .forEach { with(it) { drawToViewport() } }
            }
        )
    }
}

private const val CLIPPING_BORDER = 20f