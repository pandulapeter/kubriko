package com.pandulapeter.gameTemplate.engine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import com.pandulapeter.gameTemplate.engine.EngineImpl._fps
import com.pandulapeter.gameTemplate.engine.EngineImpl._isFocused
import com.pandulapeter.gameTemplate.engine.EngineImpl.lastFpsUpdateTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive

interface Engine {
    val isFocused: StateFlow<Boolean>
    val fps: StateFlow<Float>
}

@Composable
fun EngineCanvas(
    update: Engine.(deltaTimeInMillis: Float) -> Unit,
    draw: DrawScope.() -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(Unit) {
        var gameTime = 0L
        while (isActive) {
            withFrameNanos { gameTimeNanos ->
                val deltaTimeMillis = (gameTimeNanos - gameTime) / 1000000f
                gameTime = gameTimeNanos
                EngineImpl.updateFocus(
                    isFocused = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
                )
                EngineImpl.updateFps(
                    gameTimeNanos = gameTimeNanos,
                    deltaTimeMillis = deltaTimeMillis,
                )
                update(EngineImpl, deltaTimeMillis)
            }
        }
    }

    Canvas(
        modifier = Modifier.fillMaxSize(),
        onDraw = draw,
    )
}

fun getEngine() : Engine = EngineImpl

internal object EngineImpl : Engine {
    private val _isFocused = MutableStateFlow(false)
    override val isFocused = _isFocused.asStateFlow()
    private val _fps = MutableStateFlow(0f)
    override val fps = _fps.asStateFlow()
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
}