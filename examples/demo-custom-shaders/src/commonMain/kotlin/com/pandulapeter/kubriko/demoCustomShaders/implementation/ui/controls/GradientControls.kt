package com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.controls

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.GradientShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.FloatSlider

@Composable
internal fun GradientControls(
    gradientShaderState: GradientShader.State,
    onGradientShaderStateChanged: (GradientShader.State) -> Unit,
) {
    FloatSlider(
        title = "Speed",
        value = gradientShaderState.speed,
        onValueChanged = { onGradientShaderStateChanged(gradientShaderState.copy(speed = it)) },
        valueRange = 0f..8f,
    )
    FloatSlider(
        title = "Dark",
        value = gradientShaderState.dark,
        onValueChanged = { onGradientShaderStateChanged(gradientShaderState.copy(dark = it)) },
        valueRange = 0f..0.5f,
    )
    FloatSlider(
        title = "Freq",
        value = gradientShaderState.frequency,
        onValueChanged = { onGradientShaderStateChanged(gradientShaderState.copy(frequency = it)) },
        valueRange = 0f..8f,
    )
}