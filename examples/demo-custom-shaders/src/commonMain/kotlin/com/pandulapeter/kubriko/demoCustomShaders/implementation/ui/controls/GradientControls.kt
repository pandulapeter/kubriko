package com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.controls

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.GradientShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.FloatSlider

@Composable
internal fun GradientControls(
    properties: GradientShader.State,
    onPropertiesChanged: (GradientShader.State) -> Unit,
) {
    FloatSlider(
        title = "Speed",
        value = properties.speed,
        onValueChanged = { onPropertiesChanged(properties.copy(speed = it)) },
        valueRange = 0f..10f,
    )
    FloatSlider(
        title = "Dark",
        value = properties.dark,
        onValueChanged = { onPropertiesChanged(properties.copy(dark = it)) },
        valueRange = 0f..1f,
    )
    FloatSlider(
        title = "Freq",
        value = properties.frequency,
        onValueChanged = { onPropertiesChanged(properties.copy(frequency = it)) },
        valueRange = 0f..20f,
    )
}