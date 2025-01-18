/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.controls

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.WarpShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.FloatSlider
import kotlin.math.roundToInt

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
    FloatSlider(
        title = "Light",
        value = warpShaderState.light.toFloat(),
        onValueChanged = { onWarpShaderStateChanged(warpShaderState.copy(light = it.roundToInt())) },
        valueRange = 50f..100f,
    )
}