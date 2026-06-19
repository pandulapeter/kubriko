/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.helpers.extensions.toHSV
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorText
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.settings.ColorEditorMode
import com.pandulapeter.kubriko.uiComponents.TextInput
import kotlin.math.roundToInt

@Composable
internal fun ColorPropertyEditor(
    name: String,
    value: Color,
    onValueChanged: (Color) -> Unit,
    colorEditorMode: ColorEditorMode,
) = Column(
    modifier = Modifier.fillMaxWidth(),
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EditorText(
            text = name,
            isBold = true,
        )
        Row {
            EditorText(
                text = "#",
            )
            HexInput(
                modifier = Modifier.weight(1f),
                value = value,
                onValueChanged = onValueChanged,
            )
        }
    }
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
private fun HexInput(
    modifier: Modifier,
    value: Color,
    onValueChanged: (Color) -> Unit,
) {
    val hexValue = value.toHexString()
    var text by remember(hexValue) { mutableStateOf(hexValue) }
    TextInput(
        modifier = modifier,
        value = text,
        onValueChanged = { input ->
            text = input.filter { it.isHexDigit() }.uppercase().take(6)
            text.parseHexColor(value.alpha)?.let(onValueChanged)
        },
    )
}

private fun Char.isHexDigit() = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private fun Float.toHexChannel() = (this * 255).roundToInt().coerceIn(0, 255).toString(16).padStart(2, '0').uppercase()

private fun Color.toHexString() = "${red.toHexChannel()}${green.toHexChannel()}${blue.toHexChannel()}"

private fun String.parseHexColor(alpha: Float) = takeIf { it.length == 6 }?.toLongOrNull(16)?.let { value ->
    Color(
        red = ((value shr 16) and 0xFF) / 255f,
        green = ((value shr 8) and 0xFF) / 255f,
        blue = (value and 0xFF) / 255f,
        alpha = alpha,
    )
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