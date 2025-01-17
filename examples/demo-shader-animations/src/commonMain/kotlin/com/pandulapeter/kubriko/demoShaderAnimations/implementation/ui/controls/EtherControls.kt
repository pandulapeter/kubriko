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
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.EtherShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.FloatSlider

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
    FloatSlider(
        title = "Focus",
        value = etherShaderState.focus,
        onValueChanged = { onEtherShaderStateChanged(etherShaderState.copy(focus = it)) },
        valueRange = 0f..6.5f,
    )
}