package com.pandulapeter.gameTemplate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import game.app.generated.resources.Res
import game.app.generated.resources.logo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private val rectangleOffsets = (0..360)
private val showContent = MutableStateFlow(false)

@Composable
@Preview
fun App(
    modifier: Modifier = Modifier,
) {
    MaterialTheme {
        var angle by remember { mutableStateOf(0f) }
        var fps by remember { mutableStateOf(0f) }
        var lastFpsUpdateTimestamp = 0L
        val lifecycle = LocalLifecycleOwner.current.lifecycle

        fun update(gameTimeNanos: Long, deltaTimeMillis: Float) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                angle += 0.1f * deltaTimeMillis
                if (gameTimeNanos - lastFpsUpdateTimestamp >= 1000000000L) {
                    fps = if (deltaTimeMillis == 0f) fps else 1000f / deltaTimeMillis
                    lastFpsUpdateTimestamp = gameTimeNanos
                }
            }
        }


        LaunchedEffect(Unit) {
            var gameTime = 0L
            while (isActive) {
                withFrameNanos {
                    update(it, (it - gameTime)/1000000f)
                    gameTime = it
                }
            }
        }

        Canvas(
            modifier = Modifier.fillMaxSize(),
            onDraw = {
                for (offset in rectangleOffsets) {
                    withTransform({
                        translate(
                            left = size.width / 2f,
                            top = -size.height + offset,
                        )
                        rotate(degrees = angle + offset, pivot = Offset.Zero)
                    }) {
                        var hue = offset * 1.21f
                        while (hue > 360f) {
                            hue -= 360f
                        }
                        drawRect(
                            color = Color.hsv(hue, 1f, 0.9f),
                            size = size * 2.5f
                        )
                    }
                }
            }
        )
        Column(
            modifier = modifier.fillMaxSize().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { showContent.update { currentValue -> !currentValue } },
            ) {
                Text("Click me!")
            }
            FPSCounter(
                fps = fps,
            )
            val shouldShowContent = showContent.collectAsState()
            AnimatedVisibility(
                visible = shouldShowContent.value,
            ) {
                val greeting = remember { Greeting().greet() }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.logo),
                        contentDescription = null,
                    )
                    Text(
                        text = "Compose: $greeting",
                    )
                }
            }
        }
    }
}

@Composable
private fun FPSCounter(
    fps: Float
) = Text(
    text = "FPS: ${fps.toString().subSequence(0, fps.toString().indexOf('.'))}",
)