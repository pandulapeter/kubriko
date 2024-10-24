package com.pandulapeter.gameTemplate.gameplay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import com.pandulapeter.gameTemplate.engine.EngineCanvas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private val rectangleOffsets = (0..360)
private val angle = MutableStateFlow(0f)

@Composable
fun GameplayCanvas() {
    val angleState = angle.collectAsState()
    EngineCanvas(
        update = { deltaTimeMillis ->
            if (isFocused.value) {
                angle.update { currentValue -> currentValue + 0.1f * deltaTimeMillis }
            }
        },
        draw = {
            for (offset in rectangleOffsets) {
                withTransform(
                    transformBlock = {
                        translate(
                            left = size.width / 2f,
                            top = -size.height + offset,
                        )
                        rotate(
                            degrees = angleState.value + offset,
                            pivot = Offset.Zero,
                        )
                    },
                    drawBlock = {
                        var hue = offset * 1.21f
                        while (hue > 360f) {
                            hue -= 360f
                        }
                        drawRect(
                            color = Color.hsv(hue, 1f, 0.9f),
                            size = size * 2.5f
                        )
                    },
                )
            }
        }
    )
}