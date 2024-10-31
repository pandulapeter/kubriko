package com.pandulapeter.kubriko.editor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.editor.implementation.userInterface.components.EditorSlider
import com.pandulapeter.kubriko.engine.implementation.extensions.deg
import com.pandulapeter.kubriko.engine.types.AngleDegrees

@Composable
internal fun AngleDegreesPropertyEditor(
    name: String,
    value: AngleDegrees,
    onValueChanged: (AngleDegrees) -> Unit,
) = EditorSlider(
    modifier = Modifier.padding(horizontal = 8.dp),
    title = name,
    value = value.normalized,
    onValueChange = { onValueChanged(it.deg) },
    valueRange = 0f..359.99f
)