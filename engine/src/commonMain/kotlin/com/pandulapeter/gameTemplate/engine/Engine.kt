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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive

interface Engine {
    val isFocused: StateFlow<Boolean>
    val fps: StateFlow<Float>
    val drawnObjectCount: StateFlow<Int>
    val cameraOffset: StateFlow<Offset>

    fun addToCameraOffset(offset: Offset)
}

@Composable
fun EngineCanvas(
    gameObjects: List<GameObject>,
    onKey: (key: Key) -> Unit = {},
    onKeyRelease: (key: Key) -> Unit = {},
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val gameTime = remember { mutableStateOf(0L) }
    val focusRequester = remember { FocusRequester() }
    val activeKeys = mutableSetOf<Key>()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        while (isActive) {
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
                activeKeys.forEach(onKey)
                if (EngineImpl.isFocused.value) {
                    gameObjects.forEach { it.update(deltaTimeMillis) }
                }
                gameTime.value = gameTimeNanos
            }
        }
    }

    var objectCount: Int
    Canvas(
        modifier = Modifier.fillMaxSize()
            .onKeyEvent {
                consume {
                    if (it.type == KeyEventType.KeyDown) {
                        activeKeys.add(it.key)
                    }
                    if (it.type == KeyEventType.KeyUp) {
                        activeKeys.remove(it.key)
                        onKeyRelease(it.key)
                    }
                }
            }
            .focusRequester(focusRequester)
            .focusable(),
        onDraw = {
            gameTime.value
            objectCount = 0
            EngineImpl.cameraOffset.value.let { cameraOffset ->
                gameObjects.forEach { gameObject ->
                    if (
                        gameObject.isVisible(
                            screenSize = size,
                            cameraOffset = cameraOffset,
                        )
                    ) {
                        objectCount++
                        withTransform(
                            transformBlock = {
                                translate(
                                    left = gameObject.position.x - gameObject.pivot.x - cameraOffset.x + size.width / 2f,
                                    top = gameObject.position.y - gameObject.pivot.y - cameraOffset.y + size.height / 2f,
                                )
                                rotate(
                                    degrees = gameObject.rotationDegrees,
                                    pivot = gameObject.pivot,
                                )
                                scale(
                                    scaleX = gameObject.scale.scaleX,
                                    scaleY = gameObject.scale.scaleY,
                                    pivot = gameObject.pivot,
                                )
                            },
                            drawBlock = {
                                gameObject.draw(this)
                            }
                        )
                    }
                }
                EngineImpl.updateDrawnObjectCount(objectCount)
            }
        }
    )
}

fun getEngine(): Engine = EngineImpl

private fun GameObject.isVisible(
    screenSize: Size,
    cameraOffset: Offset,
) = (size.width * scale.scaleX).let { scaledWidth ->
    (size.height * scale.scaleY).let { scaledHeight ->
        position.x - pivot.x + scaledWidth >= cameraOffset.x - screenSize.width / 2f &&
                position.x - pivot.x - scaledWidth <= cameraOffset.x + screenSize.width - screenSize.width / 2f &&
                position.y - pivot.y + scaledHeight >= cameraOffset.y - screenSize.height / 2f &&
                position.y - pivot.y - scaledHeight <= cameraOffset.y + screenSize.height - screenSize.height / 2f
    }
}

internal object EngineImpl : Engine {
    private val _isFocused = MutableStateFlow(false)
    override val isFocused = _isFocused.asStateFlow()
    private val _fps = MutableStateFlow(0f)
    override val fps = _fps.asStateFlow()
    private val _drawnObjectCount = MutableStateFlow(0)
    override val drawnObjectCount = _drawnObjectCount.asStateFlow()
    private val _cameraOffset = MutableStateFlow(Offset.Zero)
    override val cameraOffset = _cameraOffset.asStateFlow()
    private var lastFpsUpdateTimestamp = 0L

    override fun addToCameraOffset(
        offset: Offset,
    ) = _cameraOffset.update { currentValue -> currentValue + offset }

    fun updateFocus(
        isFocused: Boolean,
    ) = _isFocused.update { isFocused }

    fun updateFps(
        gameTimeNanos: Long,
        deltaTimeMillis: Float,
    ) {
        if (gameTimeNanos - lastFpsUpdateTimestamp >= 1000000000L) {
            _fps.update { currentValue ->
                if (deltaTimeMillis == 0f) currentValue else 1000f / deltaTimeMillis
            }
            lastFpsUpdateTimestamp = gameTimeNanos
        }
    }

    fun updateDrawnObjectCount(
        drawnObjectCount: Int,
    ) = _drawnObjectCount.update { drawnObjectCount }
}