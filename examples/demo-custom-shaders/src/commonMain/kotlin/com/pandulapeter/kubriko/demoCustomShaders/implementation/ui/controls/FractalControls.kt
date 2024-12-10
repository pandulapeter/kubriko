package com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.controls

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.FractalShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.ColorSlider
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.FloatSlider
import kotlin.math.roundToInt

@Composable
internal fun FractalControls(
    fractalShaderState: FractalShader.State,
    onFractalShaderStateChanged: (FractalShader.State) -> Unit,
) {
    FloatSlider(
        title = "Speed",
        value = fractalShaderState.speed,
        onValueChanged = { onFractalShaderStateChanged(fractalShaderState.copy(speed = it)) },
        valueRange = 0f..20f,
    )
    ColorSlider(
        title = "Color",
        red = fractalShaderState.red.toFloat(),
        green = fractalShaderState.green.toFloat(),
        blue = fractalShaderState.blue.toFloat(),
        onValueChanged = { red, green, blue ->
            onFractalShaderStateChanged(
                fractalShaderState.copy(
                    red = red.roundToInt(),
                    green = green.roundToInt(),
                    blue = blue.roundToInt(),
                )
            )
        },
        valueRange = 0f..20f,
    )
}