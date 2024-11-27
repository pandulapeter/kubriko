package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.implementation.extensions.deg
import com.pandulapeter.kubriko.types.AngleDegrees

@Composable
internal fun AngleDegreesPropertyEditor(
    name: String,
    value: AngleDegrees,
    onValueChanged: (AngleDegrees) -> Unit,
) = FloatPropertyEditor(
    name = name,
    suffix = "°",
    value = value.normalized,
    onValueChanged = { onValueChanged(it.deg) },
    valueRange = 0f..359.99f
)