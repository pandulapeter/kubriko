package com.pandulapeter.gameTemplate.engine

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
import com.pandulapeter.gameTemplate.engine.implementation.extensions.minus
import com.pandulapeter.gameTemplate.engine.implementation.extensions.transform
import com.pandulapeter.gameTemplate.engine.implementation.extensions.transformViewport
import com.pandulapeter.gameTemplate.engine.implementation.helpers.rememberKeyboardEventHandler
import com.pandulapeter.gameTemplate.engine.implementation.managers.InputManagerImpl
import com.pandulapeter.gameTemplate.engine.implementation.managers.InstanceManagerImpl
import com.pandulapeter.gameTemplate.engine.implementation.managers.MetadataManagerImpl
import com.pandulapeter.gameTemplate.engine.implementation.managers.StateManagerImpl
import com.pandulapeter.gameTemplate.engine.implementation.managers.ViewportManagerImpl
import kotlinx.coroutines.isActive

@Composable
fun EngineCanvas(
    modifier: Modifier = Modifier,
    engine: Engine,
) {
    // Caching the internal implementations
    val inputManager = remember { engine.inputManager as InputManagerImpl }
    val instanceManager = remember { engine.instanceManager as InstanceManagerImpl }
    val metadataManager = remember { engine.metadataManager as MetadataManagerImpl }
    val stateManager = remember { engine.stateManager as StateManagerImpl }
    val viewportManager = remember { engine.viewportManager as ViewportManagerImpl }

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
                    instanceManager.dynamicGameObjects.value.forEach { dynamic ->
                        dynamic.update(deltaTimeInMillis)
                    }
                }
                gameTime.value = gameTimeNanos
            }
        }
    }

    // Keyboard event handling
    val keyboardEventHandler = (engine.inputManager as InputManagerImpl).let { inputManager ->
        rememberKeyboardEventHandler(
            onKeyPressed = inputManager::onKeyPressed,
            onKeyReleased = inputManager::onKeyReleased,
        )
    }
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
            viewportManager.center.value.let { viewportCenter ->
                withTransform(
                    transformBlock = {
                        transformViewport(
                            viewportCenter = viewportCenter,
                            shiftedViewportOffset = (size / 2f) - viewportCenter,
                            viewportScaleFactor = viewportManager.scaleFactor.value,
                        )
                    },
                    drawBlock = {
                        instanceManager.visibleInstancesWithinViewport.value.forEach { visible ->
                            withTransform(
                                transformBlock = { visible.transform(this) },
                                drawBlock = { visible.draw(this) }
                            )
                        }
                    }
                )
            }
        }
    )
}

