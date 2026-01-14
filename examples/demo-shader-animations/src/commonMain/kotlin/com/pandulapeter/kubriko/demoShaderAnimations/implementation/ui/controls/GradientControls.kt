/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.controls

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.GradientShader
import com.pandulapeter.kubriko.uiComponents.SmallSliderWithTitle
import kubriko.examples.demo_shader_animations.generated.resources.Res
import kubriko.examples.demo_shader_animations.generated.resources.dark
import kubriko.examples.demo_shader_animations.generated.resources.frequency
import kubriko.examples.demo_shader_animations.generated.resources.speed
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GradientControls(
    gradientShaderState: GradientShader.State,
    onGradientShaderStateChanged: (GradientShader.State) -> Unit,
) {
    SmallSliderWithTitle(
        title = stringResource(Res.string.speed),
        value = gradientShaderState.speed,
        onValueChanged = { onGradientShaderStateChanged(gradientShaderState.copy(speed = it)) },
        valueRange = 0f..8f,
    )
    SmallSliderWithTitle(
        title = stringResource(Res.string.dark),
        value = gradientShaderState.dark,
        onValueChanged = { onGradientShaderStateChanged(gradientShaderState.copy(dark = it)) },
        valueRange = 0f..0.5f,
    )
    SmallSliderWithTitle(
        title = stringResource(Res.string.frequency),
        value = gradientShaderState.frequency,
        onValueChanged = { onGradientShaderStateChanged(gradientShaderState.copy(frequency = it)) },
        valueRange = 0f..8f,
    )
}