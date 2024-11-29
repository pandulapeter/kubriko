package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.implementation.extensions.toHSV
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorRadioButton
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorText

@Composable
internal fun ColorPropertyEditor(
    name: String,
    value: Color,
    onValueChanged: (Color) -> Unit,
    colorEditorMode: ColorEditorMode,
    onColorEditorModeChanged: (ColorEditorMode) -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth(),
) {
    EditorText(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        text = name,
        isBold = true,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color = value),
            )
            Column(
                modifier = Modifier.weight(1f).padding(end = 4.dp),
            ) {
                ColorEditorMode.entries.forEach { mode ->
                    EditorRadioButton(
                        label = when (mode) {
                            ColorEditorMode.HSV -> "HSV"
                            ColorEditorMode.RGB -> "RGB"
                        },
                        isSmall = true,
                        isSelected = mode == colorEditorMode,
                        onSelectionChanged = { onColorEditorModeChanged(mode) },
                    )
                }
            }
        }
        FloatPropertyEditor(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
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
    )
    FloatPropertyEditor(
        name = "saturation",
        value = colorSaturation,
        onValueChanged = { onValueChanged(Color.hsv(colorHue, it, colorValue).copy(alpha = value.alpha)) },
        valueRange = 0f..1f,
        enabled = colorValue > 0,
    )
    FloatPropertyEditor(
        name = "value",
        value = colorValue,
        onValueChanged = { onValueChanged(Color.hsv(colorHue, colorSaturation, it).copy(alpha = value.alpha)) },
        valueRange = 0f..1f,
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
    )
    FloatPropertyEditor(
        name = "green",
        value = value.green,
        onValueChanged = { onValueChanged(value.copy(green = it)) },
        valueRange = 0f..1f,
    )
    FloatPropertyEditor(
        name = "blue",
        value = value.blue,
        onValueChanged = { onValueChanged(value.copy(blue = it)) },
        valueRange = 0f..1f,
    )
}

internal enum class ColorEditorMode {
    HSV,
    RGB,
}