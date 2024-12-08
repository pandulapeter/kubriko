package com.pandulapeter.kubriko.implementation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.fold
import com.pandulapeter.kubriko.implementation.extensions.minus
import com.pandulapeter.kubriko.implementation.extensions.transformForViewport
import com.pandulapeter.kubriko.implementation.extensions.transformViewport
import com.pandulapeter.kubriko.manager.ViewportManager
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive

@Composable
fun InternalViewport(
    modifier: Modifier = Modifier,
    getKubriko: () -> Kubriko,
) {
    // Enforce and cache the internal implementation
    val kubrikoImpl = remember {
        getKubriko() as? KubrikoImpl ?: throw IllegalStateException("Custom Kubriko implementations are not supported. Use Kubriko.newInstance() to instantiate Kubriko.")
    }

    // Game loop and focus handling
    val gameTime = remember { mutableStateOf(0L) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { gameTimeInNanos ->
                val deltaTimeInMillis = if (gameTime.value == 0L) 0f else (gameTimeInNanos - gameTime.value) / 1000000f
                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED).let(kubrikoImpl.stateManager::updateFocus)
                kubrikoImpl.managers.forEach { it.onUpdate(deltaTimeInMillis, gameTimeInNanos) }
                gameTime.value = gameTimeInNanos
            }
        }
    }
    kubrikoImpl.managers.forEach { it.onRecomposition() }
    DisposableEffect(Unit) {
        kubrikoImpl.managers.forEach { it.onLaunch() }
        onDispose { kubrikoImpl.managers.forEach { it.onDispose() } }
    }

    // Game canvas
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(
            modifier = when (val aspectRatioMode = kubrikoImpl.viewportManager.aspectRatioMode) {
                ViewportManager.AspectRatioMode.Dynamic,
                    // TODO: Bar color should be customizable
                is ViewportManager.AspectRatioMode.FitHorizontal,
                is ViewportManager.AspectRatioMode.FitVertical -> Modifier

                is ViewportManager.AspectRatioMode.Fixed -> Modifier.align(Alignment.Center).aspectRatio(
                    ratio = aspectRatioMode.ratio,
                )
            }
        ) {
            val density = LocalDensity.current
            LaunchedEffect(maxWidth, maxHeight) {
                with(density) {
                    kubrikoImpl.viewportManager.run {
                        val newSize = Size(maxWidth.toPx(), maxHeight.toPx())
                        updateSize(newSize)
                        scaleFactorMultiplier.update {
                            when (val aspectRatioMode = aspectRatioMode) {
                                ViewportManager.AspectRatioMode.Dynamic -> 1f
                                is ViewportManager.AspectRatioMode.FitHorizontal -> maxWidth.toPx() / aspectRatioMode.defaultWidth
                                is ViewportManager.AspectRatioMode.FitVertical -> maxHeight.toPx() / aspectRatioMode.defaultHeight
                                is ViewportManager.AspectRatioMode.Fixed -> maxWidth.toPx() / aspectRatioMode.defaultWidth
                            }
                        }
                    }
                }
            }
            LayerCluster(
                gameTime = gameTime.value,
                getKubriko = { kubrikoImpl },
            )
        }
    }
}

@Composable
private fun LayerCluster(
    gameTime: Long,
    getKubriko: () -> KubrikoImpl,
) = getKubriko().let { kubrikoImpl ->
    Box(
        modifier = kubrikoImpl.managers.mapNotNull { it.getModifier(null) }.toImmutableList().fold()
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
                kubrikoImpl.managers.mapNotNull { it.getModifier(layerIndex) }.toImmutableList().fold()
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
                                                right = body.size.width.raw,
                                                bottom = body.size.height.raw,
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