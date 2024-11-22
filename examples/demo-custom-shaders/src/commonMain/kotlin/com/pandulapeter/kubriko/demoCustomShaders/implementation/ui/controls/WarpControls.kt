package com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.controls

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.WarpShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.FloatSlider

@Composable
internal fun WarpControls(
    properties: WarpShader.State,
    onPropertiesChanged: (WarpShader.State) -> Unit,
) {
    FloatSlider(
        title = "Speed",
        value = properties.speed,
        onValueChanged = { onPropertiesChanged(properties.copy(speed = it)) },
        valueRange = 0f..100f,
    )
}