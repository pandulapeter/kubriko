package com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.controls

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.CloudShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.ColorSlider
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.FloatSlider

@Composable
internal fun CloudControls(
    properties: CloudShader.State,
    onPropertiesChanged: (CloudShader.State) -> Unit,
) {
    FloatSlider(
        title = "Scale",
        value = properties.scale,
        onValueChanged = { onPropertiesChanged(properties.copy(scale = it)) },
        valueRange = 0.5f..5f,
    )
    FloatSlider(
        title = "Speed",
        value = properties.speed,
        onValueChanged = { onPropertiesChanged(properties.copy(speed = it)) },
    )
    FloatSlider(
        title = "Dark",
        value = properties.dark,
        onValueChanged = { onPropertiesChanged(properties.copy(dark = it)) },
    )
    FloatSlider(
        title = "Light",
        value = properties.light,
        onValueChanged = { onPropertiesChanged(properties.copy(light = it)) },
    )
    FloatSlider(
        title = "Cover",
        value = properties.cover,
        onValueChanged = { onPropertiesChanged(properties.copy(cover = it)) },
        valueRange = 0f..4f,
    )
    FloatSlider(
        title = "Alpha",
        value = properties.alpha,
        onValueChanged = { onPropertiesChanged(properties.copy(alpha = it)) },
        valueRange = 0f..10f,
    )
    ColorSlider(
        title = "Sky 1",
        red = properties.sky1Red,
        green = properties.sky1Green,
        blue = properties.sky1Blue,
        onValueChanged = { red, green, blue ->
            onPropertiesChanged(
                properties.copy(
                    sky1Red = red,
                    sky1Green = green,
                    sky1Blue = blue
                )
            )
        },
        valueRange = 0f..1f,
    )
    ColorSlider(
        title = "Sky 2",
        red = properties.sky2Red,
        green = properties.sky2Green,
        blue = properties.sky2Blue,
        onValueChanged = { red, green, blue ->
            onPropertiesChanged(
                properties.copy(
                    sky2Red = red,
                    sky2Green = green,
                    sky2Blue = blue
                )
            )
        },
        valueRange = 0f..1f,
    )
}