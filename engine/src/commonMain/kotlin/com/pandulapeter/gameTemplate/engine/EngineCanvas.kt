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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import com.pandulapeter.gameTemplate.engine.implementation.extensions.minus
import com.pandulapeter.gameTemplate.engine.implementation.extensions.transform
import com.pandulapeter.gameTemplate.engine.implementation.helpers.rememberKeyboardEventHandler
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates
import kotlinx.coroutines.isActive

@Composable
fun EngineCanvas(
    modifier: Modifier = Modifier,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val gameTime = remember { mutableStateOf(0L) }

    val keyboardEventHandler = rememberKeyboardEventHandler(
        onKeyPressed = EngineImpl.inputManager::onKeyPressed,
        onKeyReleased = EngineImpl.inputManager::onKeyReleased,
    )

    DisposableEffect(Unit) {
        keyboardEventHandler.startListening()
        onDispose { keyboardEventHandler.stopListening() }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { gameTimeNanos ->
                val deltaTimeInMillis = (gameTimeNanos - gameTime.value) / 1000000f
                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED).let { isFocused ->
                    EngineImpl.stateManager.updateFocus(
                        isFocused = isFocused,
                    )
                }
                EngineImpl.metadataManager.updateFps(
                    gameTimeNanos = gameTimeNanos,
                    deltaTimeInMillis = deltaTimeInMillis,
                )
                EngineImpl.inputManager.emit()
                if (EngineImpl.stateManager.isRunning.value) {
                    EngineImpl.instanceManager.dynamicGameObjects.value.forEach { dynamic ->
                        dynamic.update(deltaTimeInMillis)
                    }
                }
                gameTime.value = gameTimeNanos
            }
        }
    }
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .focusable(),
        onDraw = {
            gameTime.value
            EngineImpl.viewportManager.updateSize(size = size)
            EngineImpl.viewportManager.center.value.let { viewportCenter ->
                withTransform(
                    transformBlock = {
                        transformViewport(
                            viewportCenter = viewportCenter,
                            shiftedViewportOffset = (size / 2f) - viewportCenter,
                            viewportScaleFactor = EngineImpl.viewportManager.scaleFactor.value,
                        )
                    },
                    drawBlock = {
                        EngineImpl.instanceManager.visibleGameObjectsWithinViewport.value
                            .forEach { visible ->
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

private fun DrawTransform.transformViewport(
    viewportCenter: WorldCoordinates,
    shiftedViewportOffset: WorldCoordinates,
    viewportScaleFactor: Float,
) {
    translate(
        left = shiftedViewportOffset.x,
        top = shiftedViewportOffset.y,
    )
    scale(
        scaleX = viewportScaleFactor,
        scaleY = viewportScaleFactor,
        pivot = Offset(viewportCenter.x, viewportCenter.y),
    )
}
