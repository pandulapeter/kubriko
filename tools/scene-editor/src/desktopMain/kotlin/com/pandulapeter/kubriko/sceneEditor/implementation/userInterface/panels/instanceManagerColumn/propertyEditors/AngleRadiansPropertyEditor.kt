package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.implementation.extensions.rad
import com.pandulapeter.kubriko.types.AngleRadians

@Composable
internal fun AngleRadiansPropertyEditor(
    name: String,
    value: AngleRadians,
    onValueChanged: (AngleRadians) -> Unit,
) = FloatPropertyEditor(
    name = name,
    value = value.normalized,
    onValueChanged = { onValueChanged(it.rad) },
    valueRange = 0f..6.27f
)