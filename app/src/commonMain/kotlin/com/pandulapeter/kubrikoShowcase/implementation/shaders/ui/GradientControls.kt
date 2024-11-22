package com.pandulapeter.kubrikoShowcase.implementation.shaders.ui

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.shader.collection.GradientShader

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
}