/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.controls

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.GradientShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.FloatSlider

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