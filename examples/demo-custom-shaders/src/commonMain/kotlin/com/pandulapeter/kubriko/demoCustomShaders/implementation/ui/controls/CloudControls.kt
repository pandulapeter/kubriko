package com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.controls

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.CloudShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.ColorSlider
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.FloatSlider

@Composable
internal fun CloudControls(
    cloudShaderState: CloudShader.State,
    onCloudShaderStateChanged: (CloudShader.State) -> Unit,
) {
    FloatSlider(
        title = "Scale",
        value = cloudShaderState.scale,
        onValueChanged = { onCloudShaderStateChanged(cloudShaderState.copy(scale = it)) },
        valueRange = 0.5f..5f,
    )
    FloatSlider(
        title = "Speed",
        value = cloudShaderState.speed,
        onValueChanged = { onCloudShaderStateChanged(cloudShaderState.copy(speed = it)) },
    )
    FloatSlider(
        title = "Dark",
        value = cloudShaderState.dark,
        onValueChanged = { onCloudShaderStateChanged(cloudShaderState.copy(dark = it)) },
    )
    FloatSlider(
        title = "Light",
        value = cloudShaderState.light,
        onValueChanged = { onCloudShaderStateChanged(cloudShaderState.copy(light = it)) },
    )
    FloatSlider(
        title = "Cover",
        value = cloudShaderState.cover,
        onValueChanged = { onCloudShaderStateChanged(cloudShaderState.copy(cover = it)) },
        valueRange = 0f..4f,
    )
    FloatSlider(
        title = "Alpha",
        value = cloudShaderState.alpha,
        onValueChanged = { onCloudShaderStateChanged(cloudShaderState.copy(alpha = it)) },
        valueRange = 0f..10f,
    )
    ColorSlider(
        title = "Sky 1",
        red = cloudShaderState.sky1Red,
        green = cloudShaderState.sky1Green,
        blue = cloudShaderState.sky1Blue,
        onValueChanged = { red, green, blue ->
            onCloudShaderStateChanged(
                cloudShaderState.copy(
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
        red = cloudShaderState.sky2Red,
        green = cloudShaderState.sky2Green,
        blue = cloudShaderState.sky2Blue,
        onValueChanged = { red, green, blue ->
            onCloudShaderStateChanged(
                cloudShaderState.copy(
                    sky2Red = red,
                    sky2Green = green,
                    sky2Blue = blue
                )
            )
        },
        valueRange = 0f..1f,
    )
}