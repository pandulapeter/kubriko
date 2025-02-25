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
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.CloudShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.ColorSlider
import com.pandulapeter.kubriko.uiComponents.SmallSliderWithTitle
import kubriko.examples.demo_shader_animations.generated.resources.Res
import kubriko.examples.demo_shader_animations.generated.resources.alpha
import kubriko.examples.demo_shader_animations.generated.resources.cover
import kubriko.examples.demo_shader_animations.generated.resources.dark
import kubriko.examples.demo_shader_animations.generated.resources.light
import kubriko.examples.demo_shader_animations.generated.resources.scale
import kubriko.examples.demo_shader_animations.generated.resources.sky_1
import kubriko.examples.demo_shader_animations.generated.resources.sky_2
import kubriko.examples.demo_shader_animations.generated.resources.speed
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CloudControls(
    cloudShaderState: CloudShader.State,
    onCloudShaderStateChanged: (CloudShader.State) -> Unit,
) {
    SmallSliderWithTitle(
        title = stringResource(Res.string.scale),
        value = cloudShaderState.scale,
        onValueChanged = { onCloudShaderStateChanged(cloudShaderState.copy(scale = it)) },
        valueRange = 0.5f..5f,
    )
    SmallSliderWithTitle(
        title = stringResource(Res.string.speed),
        value = cloudShaderState.speed,
        onValueChanged = { onCloudShaderStateChanged(cloudShaderState.copy(speed = it)) },
    )
    SmallSliderWithTitle(
        title = stringResource(Res.string.dark),
        value = cloudShaderState.dark,
        onValueChanged = { onCloudShaderStateChanged(cloudShaderState.copy(dark = it)) },
    )
    SmallSliderWithTitle(
        title = stringResource(Res.string.light),
        value = cloudShaderState.light,
        onValueChanged = { onCloudShaderStateChanged(cloudShaderState.copy(light = it)) },
    )
    SmallSliderWithTitle(
        title = stringResource(Res.string.cover),
        value = cloudShaderState.cover,
        onValueChanged = { onCloudShaderStateChanged(cloudShaderState.copy(cover = it)) },
        valueRange = 0f..4f,
    )
    SmallSliderWithTitle(
        title = stringResource(Res.string.alpha),
        value = cloudShaderState.alpha,
        onValueChanged = { onCloudShaderStateChanged(cloudShaderState.copy(alpha = it)) },
        valueRange = 0f..10f,
    )
    ColorSlider(
        title = stringResource(Res.string.sky_1),
        red = cloudShaderState.sky1Red,
        green = cloudShaderState.sky1Green,
        blue = cloudShaderState.sky1Blue,
        onValueChanged = { red, green, blue ->
            onCloudShaderStateChanged(
                cloudShaderState.copy(
                    sky1Red = red,
                    sky1Green = green,
                    sky1Blue = blue
                )
            )
        },
        valueRange = 0f..1f,
    )
    ColorSlider(
        title = stringResource(Res.string.sky_2),
        red = cloudShaderState.sky2Red,
        green = cloudShaderState.sky2Green,
        blue = cloudShaderState.sky2Blue,
        onValueChanged = { red, green, blue ->
            onCloudShaderStateChanged(
                cloudShaderState.copy(
                    sky2Red = red,
                    sky2Green = green,
                    sky2Blue = blue
                )
            )
        },
        valueRange = 0f..1f,
    )
}