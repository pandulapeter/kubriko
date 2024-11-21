package com.pandulapeter.kubrikoShowcase.implementation.shaders.ui

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.shader.collection.FractalShader
import kotlin.math.roundToInt

@Composable
internal fun FractalControls(
    properties: FractalShader.Properties,
    onPropertiesChanged: (FractalShader.Properties) -> Unit,
) {
    FloatSlider(
        title = "Speed",
        value = properties.speed,
        onValueChanged = { onPropertiesChanged(properties.copy(speed = it)) },
        valueRange = 0f..20f,
    )
    ColorSlider(
        title = "Color",
        red = properties.red.toFloat(),
        green = properties.green.toFloat(),
        blue = properties.blue.toFloat(),
        onValueChanged = { red, green, blue ->
            onPropertiesChanged(
                properties.copy(
                    red = red.roundToInt(),
                    green = green.roundToInt(),
                    blue = blue.roundToInt(),
                )
            )
        },
        valueRange = 0f..20f,
    )
}