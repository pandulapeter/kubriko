package com.pandulapeter.kubriko

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pandulapeter.kubriko.implementation.KubrikoImpl
import com.pandulapeter.kubriko.implementation.extensions.minus
import com.pandulapeter.kubriko.implementation.extensions.transform
import com.pandulapeter.kubriko.implementation.extensions.transformViewport
import kotlinx.coroutines.isActive

/**
 * This Composable should be embedded into applications to draw the game world and handle all related logic.
 *
 * @param kubriko - The [Kubriko] instance that will be used for the game within this Composable.
 */
@Composable
fun KubrikoCanvas(
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
    Box(
        modifier = kubrikoImpl.managers.mapNotNull { it.getModifier(null)?.collectAsState(null)?.value }
            .fold(modifier.fillMaxSize().clipToBounds()) { compoundModifier, managerModifier -> compoundModifier then managerModifier }
            .onSizeChanged { kubrikoImpl.viewportManager.updateSize(it.toSize()) },
    ) {
        kubrikoImpl.viewportManager.cameraPosition.value.let { viewportCenter ->
            kubrikoImpl.actorManager.canvasAwareActors.value.groupBy { it.canvasIndex }.let { canvasGroups ->
                canvasGroups.keys.sortedByDescending { it }.forEach { canvasIndex ->
                    Canvas(
                        modifier = (if (canvasIndex == null) emptyList() else kubrikoImpl.managers.mapNotNull { it.getModifier(canvasIndex)?.collectAsState(null)?.value })
                            .fold(Modifier.fillMaxSize().clipToBounds()) { compoundModifier, managerModifier -> compoundModifier then managerModifier },
                        onDraw = {
                            gameTime.value // This line invalidates the Canvas (causing a refresh) on every frame
                            withTransform(
                                transformBlock = {
                                    transformViewport(
                                        viewportCenter = viewportCenter,
                                        shiftedViewportOffset = (size / 2f) - viewportCenter,
                                        viewportScaleFactor = kubrikoImpl.viewportManager.scaleFactor.value,
                                    )
                                },
                                drawBlock = {
                                    kubrikoImpl.actorManager.visibleActorsWithinViewport.value
                                        .filter { it.canvasIndex == canvasIndex }
                                        .sortedByDescending { it.drawingOrder }
                                        .forEach { visible ->
                                            withTransform(
                                                transformBlock = { visible.transform(this) },
                                                drawBlock = { visible.draw(this) }
                                            )
                                        }
                                    kubrikoImpl.actorManager.overlayActors.value
                                        .filter { it.canvasIndex == canvasIndex }
                                        .sortedByDescending { it.overlayDrawingOrder }
                                        .forEach { it.drawToViewport(this) }
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

