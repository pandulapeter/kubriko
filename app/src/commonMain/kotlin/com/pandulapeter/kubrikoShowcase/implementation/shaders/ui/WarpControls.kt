package com.pandulapeter.kubrikoShowcase.implementation.shaders.ui

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.shader.collection.WarpShader

@Composable
internal fun WarpControls(
    properties: WarpShader.Properties,
    onPropertiesChanged: (WarpShader.Properties) -> Unit,
) {
    FloatSlider(
        title = "Speed",
        value = properties.speed,
        onValueChanged = { onPropertiesChanged(properties.copy(speed = it)) },
        valueRange = 0f..100f,
    )
}