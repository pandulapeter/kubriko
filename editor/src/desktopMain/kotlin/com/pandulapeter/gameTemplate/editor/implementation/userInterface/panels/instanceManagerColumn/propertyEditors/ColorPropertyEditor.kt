package com.pandulapeter.gameTemplate.editor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorSlider
import com.pandulapeter.gameTemplate.engine.implementation.extensions.toHSV

@Composable
internal fun ColorPropertyEditor(
    name: String,
    value: Color,
    onValueChanged: (Color) -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
) {
    Spacer(modifier = Modifier.height(8.dp))
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(color = value),
    )
    Spacer(modifier = Modifier.height(8.dp))
    val (colorHue, colorSaturation, colorValue) = value.toHSV()
    EditorSlider(
        title = "${name}.hue",
        value = colorHue,
        onValueChange = { onValueChanged(Color.hsv(it, colorSaturation, colorValue)) },
        valueRange = 0f..359.5f,
        enabled = colorSaturation > 0 && colorValue > 0,
    )
    EditorSlider(
        title = "${name}.saturation",
        value = colorSaturation,
        onValueChange = { onValueChanged(Color.hsv(colorHue, it, colorValue)) },
        valueRange = 0f..1f,
        enabled = colorValue > 0,
    )
    EditorSlider(
        title = "${name}.value",
        value = colorValue,
        onValueChange = { onValueChanged(Color.hsv(colorHue, colorSaturation, it)) },
        valueRange = 0f..1f,
    )
}