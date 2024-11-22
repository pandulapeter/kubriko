package com.pandulapeter.kubriko.implementation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Overlay
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.minus
import com.pandulapeter.kubriko.implementation.extensions.transform
import com.pandulapeter.kubriko.implementation.extensions.transformViewport
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.isActive

@Composable
fun InternalViewport(
    modifier: Modifier = Modifier,
    kubriko: Kubriko,
) {
    // Enforce and cache the internal implementation
    val kubrikoImpl = remember(kubriko) {
        kubriko as? KubrikoImpl ?: throw IllegalStateException("Custom Kubriko implementations are not supported. Use Kubriko.newInstance() to instantiate Kubriko.")
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
    BoxWithConstraints(
        modifier = modifier,
    ) {
        with(LocalDensity.current) {
            kubrikoImpl.viewportManager.updateSize(Size(maxWidth.toPx(), maxHeight.toPx()))
        }
        CanvasWrapperCluster(
            gameTime = gameTime.value,
            canvasModifiers = {
                kubrikoImpl.actorManager.canvasIndices.value.associateWith { canvasIndex ->
                    kubrikoImpl.managers
                        .mapNotNull { it.getModifier(canvasIndex) }
                        .fold<Modifier, Modifier>(Modifier) { compoundModifier, managerModifier -> compoundModifier then managerModifier }
                }.toImmutableMap()
            },
            viewportSize = { kubrikoImpl.viewportManager.size.value },
            viewportCenter = { kubrikoImpl.viewportManager.cameraPosition.value },
            viewportScaleFactor = { kubrikoImpl.viewportManager.scaleFactor.value },
            visibleActorsWithinViewport = { kubrikoImpl.actorManager.visibleActorsWithinViewport.value },
            overlayActors = { kubrikoImpl.actorManager.overlayActors.value },
        )
    }
}

@Composable
private fun CanvasWrapperCluster(
    gameTime: Long,
    canvasModifiers: () -> ImmutableMap<Int?, Modifier>,
    viewportSize: () -> Size,
    viewportCenter: () -> SceneOffset,
    viewportScaleFactor: () -> Float,
    visibleActorsWithinViewport: () -> ImmutableList<Visible>,
    overlayActors: () -> ImmutableList<Overlay>,
) = canvasModifiers().let { resolvedCanvasModifiers ->
    Box(
        modifier = resolvedCanvasModifiers[null] ?: Modifier,
    ) {
        resolvedCanvasModifiers.forEach { (canvasIndex, modifier) ->
            CanvasWrapper(
                gameTime = gameTime,
                modifier = { if (canvasIndex == null) Modifier else modifier },
                viewportSize = viewportSize,
                viewportCenter = viewportCenter,
                viewportScaleFactor = viewportScaleFactor,
                visibleActorsWithinViewport = { visibleActorsWithinViewport().filter { it.canvasIndex == canvasIndex }.toImmutableList() },
                overlayActors = { overlayActors().filter { it.canvasIndex == canvasIndex }.toImmutableList() },
            )
        }
    }
}

@Composable
private fun CanvasWrapper(
    gameTime: Long,
    modifier: () -> Modifier,
    viewportSize: () -> Size,
    viewportCenter: () -> SceneOffset,
    viewportScaleFactor: () -> Float,
    visibleActorsWithinViewport: () -> ImmutableList<Visible>,
    overlayActors: () -> ImmutableList<Overlay>,
) = viewportCenter().let { resolvedViewportCenter ->
    Canvas(
        modifier = modifier().fillMaxSize().clipToBounds(),
        onDraw = {
            gameTime // This line invalidates the Canvas (causing a refresh) on every frame
            withTransform(
                transformBlock = {
                    transformViewport(
                        viewportCenter = resolvedViewportCenter,
                        shiftedViewportOffset = (viewportSize() / 2f) - resolvedViewportCenter,
                        viewportScaleFactor = viewportScaleFactor(),
                    )
                },
                drawBlock = {
                    visibleActorsWithinViewport().forEach { visible ->
                        withTransform(
                            transformBlock = { visible.transform(this) },
                            drawBlock = {
                                with(visible) {
                                    clipRect(
                                        right = boundingBox.width.raw,
                                        bottom = boundingBox.height.raw,
                                    ) {
                                        draw()
                                    }
                                }
                            }
                        )
                    }
                }
            )
            overlayActors().forEach { with(it) { drawToViewport() } }
        }
    )
}