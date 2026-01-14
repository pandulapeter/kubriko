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
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.WarpShader
import com.pandulapeter.kubriko.uiComponents.SmallSliderWithTitle
import kubriko.examples.demo_shader_animations.generated.resources.Res
import kubriko.examples.demo_shader_animations.generated.resources.light
import kubriko.examples.demo_shader_animations.generated.resources.speed
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun WarpControls(
    warpShaderState: WarpShader.State,
    onWarpShaderStateChanged: (WarpShader.State) -> Unit,
) {
    SmallSliderWithTitle(
        title = stringResource(Res.string.speed),
        value = warpShaderState.speed,
        onValueChanged = { onWarpShaderStateChanged(warpShaderState.copy(speed = it)) },
        valueRange = 0f..100f,
    )
    SmallSliderWithTitle(
        title = stringResource(Res.string.light),
        value = warpShaderState.light.toFloat(),
        onValueChanged = { onWarpShaderStateChanged(warpShaderState.copy(light = it.roundToInt())) },
        valueRange = 50f..100f,
    )
}