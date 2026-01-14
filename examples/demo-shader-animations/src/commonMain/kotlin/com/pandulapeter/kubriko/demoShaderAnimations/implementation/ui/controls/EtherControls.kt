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
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.EtherShader
import com.pandulapeter.kubriko.uiComponents.SmallSliderWithTitle
import kubriko.examples.demo_shader_animations.generated.resources.Res
import kubriko.examples.demo_shader_animations.generated.resources.focus
import kubriko.examples.demo_shader_animations.generated.resources.speed
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EtherControls(
    etherShaderState: EtherShader.State,
    onEtherShaderStateChanged: (EtherShader.State) -> Unit,
) {
    SmallSliderWithTitle(
        title = stringResource(Res.string.speed),
        value = etherShaderState.speed,
        onValueChanged = { onEtherShaderStateChanged(etherShaderState.copy(speed = it)) },
        valueRange = 0f..10f,
    )
    SmallSliderWithTitle(
        title = stringResource(Res.string.focus),
        value = etherShaderState.focus,
        onValueChanged = { onEtherShaderStateChanged(etherShaderState.copy(focus = it)) },
        valueRange = 0f..6.5f,
    )
}