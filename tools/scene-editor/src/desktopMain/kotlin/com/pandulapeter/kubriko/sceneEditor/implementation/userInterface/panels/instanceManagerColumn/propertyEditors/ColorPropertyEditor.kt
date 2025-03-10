/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.helpers.extensions.toHSV
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorText
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.ColorEditorMode

@Composable
internal fun ColorPropertyEditor(
    name: String,
    value: Color,
    onValueChanged: (Color) -> Unit,
    colorEditorMode: ColorEditorMode,
) = Column(
    modifier = Modifier.fillMaxWidth(),
) {
    EditorText(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        text = name,
        isBold = true,
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .padding(end = 8.dp)
                .background(color = value),
        )
        FloatPropertyEditor(
            modifier = Modifier.weight(2f),
            name = "alpha",
            value = value.alpha,
            onValueChanged = { onValueChanged(value.copy(alpha = it)) },
            valueRange = 0f..1f,
        )
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
        when (colorEditorMode) {
            ColorEditorMode.HSV -> ControlsHSV(
                value = value,
                onValueChanged = onValueChanged,
            )

            ColorEditorMode.RGB -> ControlsRGB(
                value = value,
                onValueChanged = onValueChanged,
            )
        }
    }
}

@Composable
private fun ControlsHSV(
    value: Color,
    onValueChanged: (Color) -> Unit,
) {
    val (colorHue, colorSaturation, colorValue) = value.toHSV()
    FloatPropertyEditor(
        name = "hue",
        value = colorHue,
        onValueChanged = { onValueChanged(Color.hsv(it, colorSaturation, colorValue).copy(alpha = value.alpha)) },
        valueRange = 0f..359.5f,
        enabled = colorSaturation > 0 && colorValue > 0,
        shouldUseHorizontalLayout = true,
    )
    FloatPropertyEditor(
        name = "saturation",
        value = colorSaturation,
        onValueChanged = { onValueChanged(Color.hsv(colorHue, it, colorValue).copy(alpha = value.alpha)) },
        valueRange = 0f..1f,
        enabled = colorValue > 0,
        shouldUseHorizontalLayout = true,
    )
    FloatPropertyEditor(
        name = "value",
        value = colorValue,
        onValueChanged = { onValueChanged(Color.hsv(colorHue, colorSaturation, it).copy(alpha = value.alpha)) },
        valueRange = 0f..1f,
        shouldUseHorizontalLayout = true,
    )
}

@Composable
private fun ControlsRGB(
    value: Color,
    onValueChanged: (Color) -> Unit,
) {
    FloatPropertyEditor(
        name = "red",
        value = value.red,
        onValueChanged = { onValueChanged(value.copy(red = it)) },
        valueRange = 0f..1f,
        shouldUseHorizontalLayout = true,
    )
    FloatPropertyEditor(
        name = "green",
        value = value.green,
        onValueChanged = { onValueChanged(value.copy(green = it)) },
        valueRange = 0f..1f,
        shouldUseHorizontalLayout = true,
    )
    FloatPropertyEditor(
        name = "blue",
        value = value.blue,
        onValueChanged = { onValueChanged(value.copy(blue = it)) },
        valueRange = 0f..1f,
        shouldUseHorizontalLayout = true,
    )
}