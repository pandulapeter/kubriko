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
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.implementation.extensions.minus
import com.pandulapeter.kubriko.implementation.extensions.transform
import com.pandulapeter.kubriko.implementation.extensions.transformViewport
import com.pandulapeter.kubriko.implementation.manager.ActorManagerImpl
import com.pandulapeter.kubriko.implementation.manager.StateManagerImpl
import com.pandulapeter.kubriko.implementation.manager.ViewportManagerImpl
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
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
    // Caching the internal implementations
    val kubrikoImpl = remember { kubriko as KubrikoImpl }
    val actorManager = remember { kubriko.get<ActorManager>() as ActorManagerImpl }
    val stateManager = remember { kubriko.get<StateManager>() as StateManagerImpl }
    val viewportManager = remember { kubriko.get<ViewportManager>() as ViewportManagerImpl }

    // Game loop and focus handling
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val gameTime = remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { gameTimeInNanos ->
                val deltaTimeInMillis = (gameTimeInNanos - gameTime.value) / 1000000f
                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED).let(stateManager::updateFocus)
                kubrikoImpl.managers.forEach { it.update(deltaTimeInMillis, gameTimeInNanos) }
                gameTime.value = gameTimeInNanos
            }
        }
    }

    kubrikoImpl.managers.forEach { it.composition() }

    DisposableEffect(Unit) {
        kubrikoImpl.managers.forEach { it.launch() }
        onDispose { kubrikoImpl.managers.forEach { it.dispose() } }
    }

    // Game canvas
    Canvas(
        modifier = kubrikoImpl.managers
            .mapNotNull { it.modifier() }
            .fold(modifier.fillMaxSize().clipToBounds()) { compoundModifier, managerModifier ->
                compoundModifier.then(managerModifier)
            }
            .background(Color.White), // TODO: The engine should not draw its own background by default),
        onDraw = {
            gameTime.value
            viewportManager.updateSize(size = size)
            viewportManager.cameraPosition.value.let { viewportCenter ->
                withTransform(
                    transformBlock = {
                        transformViewport(
                            viewportCenter = viewportCenter,
                            shiftedViewportOffset = (size / 2f) - viewportCenter,
                            viewportScaleFactor = viewportManager.scaleFactor.value,
                        )
                    },
                    drawBlock = {
                        actorManager.visibleActorsWithinViewport.value
                            .sortedByDescending { it.drawingOrder }
                            .forEach { visible ->
                                withTransform(
                                    transformBlock = { visible.transform(this) },
                                    drawBlock = { visible.draw(this) }
                                )
                            }
                        actorManager.overlayActors.value
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

