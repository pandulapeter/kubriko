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
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.NoodleShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.ColorSlider
import com.pandulapeter.kubriko.uiComponents.SmallSliderWithTitle
import kubriko.examples.demo_shader_animations.generated.resources.Res
import kubriko.examples.demo_shader_animations.generated.resources.color
import kubriko.examples.demo_shader_animations.generated.resources.speed
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun NoodleControls(
    noodleShaderState: NoodleShader.State,
    onNoodleShaderStateChanged: (NoodleShader.State) -> Unit,
) {
    SmallSliderWithTitle(
        title = stringResource(Res.string.speed),
        value = noodleShaderState.speed,
        onValueChanged = { onNoodleShaderStateChanged(noodleShaderState.copy(speed = it)) },
        valueRange = 0f..20f,
    )
    ColorSlider(
        title = stringResource(Res.string.color),
        red = noodleShaderState.red.toFloat(),
        green = noodleShaderState.green.toFloat(),
        blue = noodleShaderState.blue.toFloat(),
        onValueChanged = { red, green, blue ->
            onNoodleShaderStateChanged(
                noodleShaderState.copy(
                    red = red.roundToInt(),
                    green = green.roundToInt(),
                    blue = blue.roundToInt(),
                )
            )
        },
        valueRange = 0f..20f,
    )
}