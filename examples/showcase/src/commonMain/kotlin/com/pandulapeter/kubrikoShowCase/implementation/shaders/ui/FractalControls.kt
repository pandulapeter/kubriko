package com.pandulapeter.kubrikoShowcase.implementation.shaders.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.shader.collection.FractalShader
import kotlin.math.roundToInt

@Composable
internal fun FractalControls(
    modifier: Modifier = Modifier,
    properties: FractalShader.Properties,
    onPropertiesChanged: (FractalShader.Properties) -> Unit,
) = Card(
    modifier = modifier,
) {
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
}