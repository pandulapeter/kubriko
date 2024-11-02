package com.pandulapeter.kubriko.engine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pandulapeter.kubriko.engine.implementation.extensions.drawEditorGrid
import com.pandulapeter.kubriko.engine.implementation.extensions.minus
import com.pandulapeter.kubriko.engine.implementation.extensions.transform
import com.pandulapeter.kubriko.engine.implementation.extensions.transformViewport
import com.pandulapeter.kubriko.engine.implementation.helpers.rememberKeyboardEventHandler
import com.pandulapeter.kubriko.engine.implementation.managers.InputManagerImpl
import com.pandulapeter.kubriko.engine.implementation.managers.ActorManagerImpl
import com.pandulapeter.kubriko.engine.implementation.managers.MetadataManagerImpl
import com.pandulapeter.kubriko.engine.implementation.managers.StateManagerImpl
import com.pandulapeter.kubriko.engine.implementation.managers.ViewportManagerImpl
import kotlinx.coroutines.isActive

/**
 * This Composable should be embedded into applications to draw the game world and handle all related logic.
 *
 * @param kubriko - The [Kubriko] instance that will be used for the game within this Composable.
 */
@Composable
fun EngineCanvas(
    modifier: Modifier = Modifier,
    kubriko: Kubriko,
) {
    // Caching the internal implementations
    val inputManager = remember { kubriko.inputManager as InputManagerImpl }
    val instanceManager = remember { kubriko.actorManager as ActorManagerImpl }
    val metadataManager = remember { kubriko.metadataManager as MetadataManagerImpl }
    val stateManager = remember { kubriko.stateManager as StateManagerImpl }
    val viewportManager = remember { kubriko.viewportManager as ViewportManagerImpl }

    // Game loop and focus handling
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val gameTime = remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { gameTimeNanos ->
                val deltaTimeInMillis = (gameTimeNanos - gameTime.value) / 1000000f
                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED).let(stateManager::updateFocus)
                metadataManager.updateFps(
                    gameTimeNanos = gameTimeNanos,
                    deltaTimeInMillis = deltaTimeInMillis,
                )
                inputManager.emit()
                if (stateManager.isRunning.value) {
                    instanceManager.dynamicActors.value.forEach { it.update(deltaTimeInMillis) }
                }
                gameTime.value = gameTimeNanos
            }
        }
    }

    // Keyboard event handling
    val keyboardEventHandler = rememberKeyboardEventHandler(
        onKeyPressed = inputManager::onKeyPressed,
        onKeyReleased = inputManager::onKeyReleased,
    )
    DisposableEffect(Unit) {
        keyboardEventHandler.startListening()
        onDispose { keyboardEventHandler.stopListening() }
    }

    // Game canvas
    Canvas(
        modifier = modifier.fillMaxSize().clipToBounds().focusable(),
        onDraw = {
            gameTime.value
            viewportManager.updateSize(size = size)
            viewportManager.size.value.let { viewportSize ->
                viewportManager.center.value.let { viewportCenter ->
                    viewportManager.scaleFactor.value.let { viewportScaleFactor ->
                        withTransform(
                            transformBlock = {
                                transformViewport(
                                    viewportCenter = viewportCenter,
                                    shiftedViewportOffset = (size / 2f) - viewportCenter,
                                    viewportScaleFactor = viewportScaleFactor,
                                )
                            },
                            drawBlock = {
                                instanceManager.visibleActorsWithinViewport.value.forEach { visible ->
                                    withTransform(
                                        transformBlock = { visible.transform(this) },
                                        drawBlock = { visible.draw(this) }
                                    )
                                }
                                if (kubriko.isEditor) {
                                    drawEditorGrid(
                                        viewportCenter = viewportCenter,
                                        viewportSize = viewportSize,
                                        viewportScaleFactor = viewportScaleFactor,
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    )
}

