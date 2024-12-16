package com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.controls

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.NoodleShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.ColorSlider
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.FloatSlider
import kotlin.math.roundToInt

@Composable
internal fun NoodleControls(
    noodleShaderState: NoodleShader.State,
    onNoodleShaderStateChanged: (NoodleShader.State) -> Unit,
) {
    FloatSlider(
        title = "Speed",
        value = noodleShaderState.speed,
        onValueChanged = { onNoodleShaderStateChanged(noodleShaderState.copy(speed = it)) },
        valueRange = 0f..20f,
    )
    ColorSlider(
        title = "Color",
        red = noodleShaderState.red.toFloat(),
        green = noodleShaderState.green.toFloat(),
        blue = noodleShaderState.blue.toFloat(),
        onValueChanged = { red, green, blue ->
            onNoodleShaderStateChanged(
                noodleShaderState.copy(
                    red = red.roundToInt(),
                    green = green.roundToInt(),
                    blue = blue.roundToInt(),
                )
            )
        },
        valueRange = 0f..20f,
    )
}