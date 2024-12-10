package com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.controls

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.demoCustomShaders.implementation.shaders.WarpShader
import com.pandulapeter.kubriko.demoCustomShaders.implementation.ui.FloatSlider

@Composable
internal fun WarpControls(
    warpShaderState: WarpShader.State,
    onWarpShaderStateChanged: (WarpShader.State) -> Unit,
) {
    FloatSlider(
        title = "Speed",
        value = warpShaderState.speed,
        onValueChanged = { onWarpShaderStateChanged(warpShaderState.copy(speed = it)) },
        valueRange = 0f..100f,
    )
}