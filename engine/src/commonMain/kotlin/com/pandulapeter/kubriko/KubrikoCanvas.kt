package com.pandulapeter.kubriko

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
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
    val kubrikoImpl = remember {
        kubriko as? KubrikoImpl
            ?: throw IllegalStateException("Custom implementations of the Kubriko interface are not supported. Use Kubriko.newInstance() to instantiate Kubriko.")
    }

    // Game loop and focus handling
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val gameTime = remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { gameTimeInNanos ->
                val deltaTimeInMillis = (gameTimeInNanos - gameTime.value) / 1000000f
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
    Canvas(
        modifier = kubrikoImpl.managers
            .mapNotNull { it.onCreateModifier() }
            .fold(modifier.fillMaxSize().clipToBounds()) { compoundModifier, managerModifier ->
                compoundModifier then managerModifier
            }
            .background(Color.White), // TODO: The engine should not draw its own background by default),
        onDraw = {
            gameTime.value
            kubrikoImpl.viewportManager.updateSize(size = size)
            kubrikoImpl.viewportManager.cameraPosition.value.let { viewportCenter ->
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
                            .sortedByDescending { it.drawingOrder }
                            .forEach { visible ->
                                withTransform(
                                    transformBlock = { visible.transform(this) },
                                    drawBlock = { visible.draw(this) }
                                )
                            }
                        kubrikoImpl.actorManager.overlayActors.value
                            .sortedByDescending { it.overlayDrawingOrder }
                            .forEach { overlay ->
                                overlay.drawToViewport(this)
                            }
                    }
                )
            }
        }
    )
}

