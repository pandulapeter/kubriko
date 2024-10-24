package com.pandulapeter.gameTemplate.engine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
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
}

@Composable
fun EngineCanvas(
    gameObjects: List<GameObject>,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val gameTime = remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { gameTimeNanos ->
                val deltaTimeMillis = (gameTimeNanos - gameTime.value) / 1000000f
                EngineImpl.updateFocus(
                    isFocused = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
                )
                EngineImpl.updateFps(
                    gameTimeNanos = gameTimeNanos,
                    deltaTimeMillis = deltaTimeMillis,
                )
                if (EngineImpl.isFocused.value) {
                    gameObjects.forEach { it.update(deltaTimeMillis) }
                }
                gameTime.value = gameTimeNanos
            }
        }
    }

    var objectCount: Int
    Canvas(
        modifier = Modifier.fillMaxSize(),
        onDraw = {
            gameTime.value
            objectCount = 0
            gameObjects.forEach { gameObject ->
                if (gameObject.isVisible(size)) {
                    objectCount++
                    withTransform(
                        transformBlock = {
                            translate(
                                left = gameObject.position.x - gameObject.pivot.x,
                                top = gameObject.position.y - gameObject.pivot.y,
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
        },
    )
}

fun getEngine(): Engine = EngineImpl

private fun GameObject.isVisible(
    screenSize: Size
) = (size.width * scale.scaleX).let { scaledWidth ->
    (size.height * scale.scaleY).let { scaledHeight ->
        position.x - pivot.x + scaledWidth >= 0 &&
        position.x - pivot.x - scaledWidth <= screenSize.width &&
        position.y - pivot.y + scaledHeight >= 0 &&
        position.y - pivot.y - scaledHeight <= screenSize.height
    }
}

internal object EngineImpl : Engine {
    private val _isFocused = MutableStateFlow(false)
    override val isFocused = _isFocused.asStateFlow()
    private val _fps = MutableStateFlow(0f)
    override val fps = _fps.asStateFlow()
    private val _drawnObjectCount = MutableStateFlow(0)
    override val drawnObjectCount = _drawnObjectCount.asStateFlow()
    private var lastFpsUpdateTimestamp = 0L

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