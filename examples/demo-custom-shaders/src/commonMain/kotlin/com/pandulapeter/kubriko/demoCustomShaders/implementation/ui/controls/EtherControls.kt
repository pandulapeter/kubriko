package com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.controls

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.EtherShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.FloatSlider

@Composable
internal fun EtherControls(
    etherShaderState: EtherShader.State,
    onEtherShaderStateChanged: (EtherShader.State) -> Unit,
) {
    FloatSlider(
        title = "Speed",
        value = etherShaderState.speed,
        onValueChanged = { onEtherShaderStateChanged(etherShaderState.copy(speed = it)) },
        valueRange = 0f..10f,
    )
}