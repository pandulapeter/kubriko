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
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color = value),
        )
        Column {
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
    Spacer(modifier = Modifier.height(8.dp))
    FloatPropertyEditor(
        name = "${name}.alpha",
        value = value.alpha,
        onValueChanged = { onValueChanged(value.copy(alpha = it)) },
        valueRange = 0f..1f,
    )
    when (colorEditorMode) {
        ColorEditorMode.HSV -> ControlsHSV(
            value = value,
            name = name,
            onValueChanged = onValueChanged,
        )

        ColorEditorMode.RGB -> ControlsRGB(
            value = value,
            name = name,
            onValueChanged = onValueChanged,
        )
    }

}

@Composable
private fun ControlsHSV(
    value: Color,
    name: String,
    onValueChanged: (Color) -> Unit,
) {
    val (colorHue, colorSaturation, colorValue) = value.toHSV()
    FloatPropertyEditor(
        name = "${name}.hue",
        value = colorHue,
        onValueChanged = { onValueChanged(Color.hsv(it, colorSaturation, colorValue).copy(alpha = value.alpha)) },
        valueRange = 0f..359.5f,
        enabled = colorSaturation > 0 && colorValue > 0,
    )
    FloatPropertyEditor(
        name = "${name}.saturation",
        value = colorSaturation,
        onValueChanged = { onValueChanged(Color.hsv(colorHue, it, colorValue).copy(alpha = value.alpha)) },
        valueRange = 0f..1f,
        enabled = colorValue > 0,
    )
    FloatPropertyEditor(
        name = "${name}.value",
        value = colorValue,
        onValueChanged = { onValueChanged(Color.hsv(colorHue, colorSaturation, it).copy(alpha = value.alpha)) },
        valueRange = 0f..1f,
    )
}

@Composable
private fun ControlsRGB(
    value: Color,
    name: String,
    onValueChanged: (Color) -> Unit,
) {
    FloatPropertyEditor(
        name = "${name}.red",
        value = value.red,
        onValueChanged = { onValueChanged(value.copy(red = it)) },
        valueRange = 0f..1f,
    )
    FloatPropertyEditor(
        name = "${name}.green",
        value = value.green,
        onValueChanged = { onValueChanged(value.copy(green = it)) },
        valueRange = 0f..1f,
    )
    FloatPropertyEditor(
        name = "${name}.blue",
        value = value.blue,
        onValueChanged = { onValueChanged(value.copy(blue = it)) },
        valueRange = 0f..1f,
    )
}

internal enum class ColorEditorMode {
    HSV,
    RGB,
}