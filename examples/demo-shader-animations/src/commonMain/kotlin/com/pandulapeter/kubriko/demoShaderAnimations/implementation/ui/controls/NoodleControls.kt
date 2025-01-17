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
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders.NoodleShader
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.ColorSlider
import com.pandulapeter.kubriko.demoShaderAnimations.implementation.ui.FloatSlider
import kotlin.math.roundToInt

@Composable
internal fun NoodleControls(
    noodleShaderState: NoodleShader.State,
    onNoodleShaderStateChanged: (NoodleShader.State) -> Unit,
) {
    FloatSlider(
        title = "Speed",
        value = noodleShaderState.speed,
        onValueChanged = { onNoodleShaderStateChanged(noodleShaderState.copy(speed = it)) },
        valueRange = 0f..20f,
    )
    ColorSlider(
        title = "Color",
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