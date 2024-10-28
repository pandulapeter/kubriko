package com.pandulapeter.gameTemplate.engine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.key.onKeyEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pandulapeter.gameTemplate.engine.gameObject.GameObject
import com.pandulapeter.gameTemplate.engine.gameObject.traits.Visible
import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import com.pandulapeter.gameTemplate.engine.implementation.extensions.minus
import com.pandulapeter.gameTemplate.engine.implementation.extensions.transform
import kotlinx.coroutines.isActive

@Composable
fun EngineCanvas(
    modifier: Modifier = Modifier,
    editorSelectedGameObjectHighlight: DrawScope.(Visible) -> Unit = {},
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val gameTime = remember { mutableStateOf(0L) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        while (isActive) {
            focusRequester.requestFocus()
            withFrameNanos { gameTimeNanos ->
                val deltaTimeMillis = (gameTimeNanos - gameTime.value) / 1000000f
                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED).let { isFocused ->
                    EngineImpl.stateManager.updateFocus(
                        isFocused = isFocused,
                    )
                }
                EngineImpl.metadataManager.updateFps(
                    gameTimeNanos = gameTimeNanos,
                    deltaTimeMillis = deltaTimeMillis,
                )
                EngineImpl.inputManager.emit()
                if (EngineImpl.stateManager.isRunning.value) {
                    EngineImpl.gameObjectManager.dynamicGameObjects.value.forEach { it.update(deltaTimeMillis) }
                }
                gameTime.value = gameTimeNanos
            }
        }
    }
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onKeyEvent(EngineImpl.inputManager::onKeyEvent)
            .focusRequester(focusRequester)
            .focusable(),
        onDraw = {
            gameTime.value
            EngineImpl.viewportManager.updateSize(size = size)
            EngineImpl.viewportManager.offset.value.let { viewportOffset ->
                withTransform(
                    transformBlock = {
                        transformViewport(
                            viewportOffset = viewportOffset,
                            shiftedViewportOffset = (size / 2f) - viewportOffset,
                            viewportScaleFactor = EngineImpl.viewportManager.scaleFactor.value,
                        )
                    },
                    drawBlock = {
                        EngineImpl.gameObjectManager.visibleGameObjectsInViewport.value.forEach { gameObject ->
                            withTransform(
                                transformBlock = { gameObject.transform(this) },
                                drawBlock = {
                                    if ((gameObject as GameObject).isSelectedInEditor) {
                                        editorSelectedGameObjectHighlight(gameObject)
                                    }
                                    gameObject.draw(this)
                                }
                            )
                        }
                    }
                )
            }
        }
    )
}

private fun DrawTransform.transformViewport(
    viewportOffset: Offset,
    shiftedViewportOffset: Offset,
    viewportScaleFactor: Float,
) {
    translate(
        left = shiftedViewportOffset.x,
        top = shiftedViewportOffset.y,
    )
    scale(
        scaleX = viewportScaleFactor,
        scaleY = viewportScaleFactor,
        pivot = viewportOffset,
    )
}
