package com.pandulapeter.gameTemplate.engine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
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
import kotlin.math.max
import kotlin.math.min

interface Engine {
    val isFocused: StateFlow<Boolean>
    val fps: StateFlow<Float>
    val drawnObjectCount: StateFlow<Int>
    val cameraOffset: StateFlow<Offset>
    val cameraScaleFactor: StateFlow<Float>

    fun addToCameraOffset(offset: Offset)

    fun multiplyCameraScaleFactor(scaleFactor: Float)
}

@Composable
fun EngineCanvas(
    gameObjects: List<GameObject>,
    onKeys: (keys: Set<Key>) -> Unit = {},
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
                if (activeKeys.isNotEmpty()) {
                    onKeys(activeKeys.toSet())
                }
                if (EngineImpl.isFocused.value) {
                    gameObjects.forEach { it.update(deltaTimeMillis) }
                }
                gameTime.value = gameTimeNanos
            }
        }
    }
    Canvas(
        modifier = Modifier.fillMaxSize()
            .background(Color.White)
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
            EngineImpl.cameraScaleFactor.value.let { cameraScaleFactor ->
                EngineImpl.cameraOffset.value.let { cameraOffsetRaw ->
                    Offset(
                        -cameraOffsetRaw.x + size.width / 2f,
                        -cameraOffsetRaw.y + size.height / 2f,
                    ).let { cameraOffset ->
                        withTransform(
                            transformBlock = {
                                translate(
                                    left = cameraOffset.x,
                                    top = cameraOffset.y,
                                )
                                scale(
                                    scaleX = cameraScaleFactor,
                                    scaleY = cameraScaleFactor,
                                    pivot = cameraOffsetRaw,
                                )
                            },
                            drawBlock = {
                                gameObjects.filter {
                                    it.isVisible(
                                        cameraScaleFactor = cameraScaleFactor,
                                        screenSize = size / cameraScaleFactor,
                                        cameraOffset = cameraOffsetRaw,
                                    )
                                }.let { visibleGameObjects ->
                                    visibleGameObjects.forEach { gameObject ->
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
                                                    scaleX = gameObject.scaleFactor,
                                                    scaleY = gameObject.scaleFactor,
                                                    pivot = gameObject.pivot,
                                                )
                                            },
                                            drawBlock = {
                                                gameObject.draw(this)
                                            }
                                        )
                                    }
                                    EngineImpl.updateDrawnObjectCount(visibleGameObjects.size)
                                }
                            }
                        )
                    }
                }
            }
        }
    )
}

fun getEngine(): Engine = EngineImpl

private fun GameObject.isVisible(
    cameraScaleFactor: Float,
    screenSize: Size,
    cameraOffset: Offset,
) = (size.width * scaleFactor).let { scaledWidth ->
    (size.height * scaleFactor).let { scaledHeight ->
        scaledWidth * cameraScaleFactor >= 1f && scaledHeight * cameraScaleFactor >= 1f &&
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
    private val _cameraScaleFactor = MutableStateFlow(1f)
    override val cameraScaleFactor = _cameraScaleFactor.asStateFlow()
    private var lastFpsUpdateTimestamp = 0L

    private const val SCALE_MIN = 0.2f
    private const val SCALE_MAX = 10f

    override fun addToCameraOffset(
        offset: Offset,
    ) = _cameraOffset.update { currentValue -> currentValue + (offset / _cameraScaleFactor.value) }

    override fun multiplyCameraScaleFactor(
        scaleFactor: Float
    ) = _cameraScaleFactor.update { currentValue -> max(SCALE_MIN, min(currentValue * scaleFactor, SCALE_MAX)) }

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