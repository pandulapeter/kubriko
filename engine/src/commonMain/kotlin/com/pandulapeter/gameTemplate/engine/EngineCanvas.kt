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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import com.pandulapeter.gameTemplate.engine.implementation.extensions.minus
import com.pandulapeter.gameTemplate.engine.implementation.extensions.proccess
import com.pandulapeter.gameTemplate.engine.implementation.extensions.transform
import kotlinx.coroutines.isActive

@Composable
fun EngineCanvas(
    modifier: Modifier = Modifier,
    handleKeys: (keys: Set<Key>) -> Unit,
    handleKeyReleased: (key: Key) -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val gameTime = remember { mutableStateOf(0L) }
    val focusRequester = remember { FocusRequester() }
    val activeKeys = mutableSetOf<Key>()

    LaunchedEffect(Unit) {
        while (isActive) {
            focusRequester.requestFocus()
            withFrameNanos { gameTimeNanos ->
                val deltaTimeMillis = (gameTimeNanos - gameTime.value) / 1000000f
                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED).let { isFocused ->
                    if (!isFocused) {
                        activeKeys.clear()
                    }
                    EngineImpl.updateFocus(
                        isFocused = isFocused,
                    )
                }
                EngineImpl.updateFps(
                    gameTimeNanos = gameTimeNanos,
                    deltaTimeMillis = deltaTimeMillis,
                )
                if (activeKeys.isNotEmpty()) {
                    handleKeys(activeKeys.toSet())
                }
                if (EngineImpl.isRunning.value) {
                    EngineImpl.dynamicGameObjects.value.forEach { it.update(deltaTimeMillis) }
                }
                gameTime.value = gameTimeNanos
            }
        }
    }
    Canvas(
        modifier = modifier.fillMaxSize()
            .onKeyEvent {
                it.proccess(
                    addToActiveKeys = activeKeys::add,
                    removeFromActiveKeys = activeKeys::remove,
                    onKeyRelease = handleKeyReleased
                )
            }
            .focusRequester(focusRequester)
            .focusable(),
        onDraw = {
            gameTime.value
            EngineImpl.updateViewportSize(size = size)
            EngineImpl.offset.value.let { viewportOffset ->
                withTransform(
                    transformBlock = {
                        transformViewport(
                            viewportOffset = viewportOffset,
                            shiftedViewportOffset = ((size / 2f) - viewportOffset),
                            viewportScaleFactor = EngineImpl.scaleFactor.value,
                        )
                    },
                    drawBlock = {
                        EngineImpl.visibleGameObjects.value.forEach { gameObject ->
                            withTransform(
                                transformBlock = { gameObject.transform(this) },
                                drawBlock = { gameObject.draw(this) }
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
