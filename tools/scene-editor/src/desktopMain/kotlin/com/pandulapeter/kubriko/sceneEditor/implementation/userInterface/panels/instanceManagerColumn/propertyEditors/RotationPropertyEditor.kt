package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.implementation.extensions.deg
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorRadioButton
import com.pandulapeter.kubriko.types.AngleRadians

@Composable
internal fun RotationPropertyEditor(
    name: String,
    value: AngleRadians,
    onValueChanged: (AngleRadians) -> Unit,
    rotationEditorMode: RotationEditorMode,
    onRotationEditorModeChanged: (RotationEditorMode) -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
) {
    when (rotationEditorMode) {
        RotationEditorMode.DEGREES -> FloatPropertyEditor(
            modifier = Modifier.weight(1f),
            name = name,
            suffix = "°",
            value = value.deg.normalized,
            onValueChanged = { onValueChanged(it.deg.rad) },
            valueRange = 0f..359.99f
        )

        RotationEditorMode.RADIANS -> FloatPropertyEditor(
            modifier = Modifier.weight(1f),
            name = name,
            value = value.normalized,
            onValueChanged = { onValueChanged(it.rad) },
            valueRange = 0f..6.27f
        )
    }
    Column(
        modifier = Modifier.width(92.dp),
    ) {
        RotationEditorMode.entries.forEach { mode ->
            EditorRadioButton(
                label = when (mode) {
                    RotationEditorMode.RADIANS -> "Radians"
                    RotationEditorMode.DEGREES -> "Degrees"
                },
                isSmall = true,
                isSelected = mode == rotationEditorMode,
                onSelectionChanged = { onRotationEditorModeChanged(mode) },
            )
        }
    }
}

internal enum class RotationEditorMode {
    DEGREES,
    RADIANS,
}