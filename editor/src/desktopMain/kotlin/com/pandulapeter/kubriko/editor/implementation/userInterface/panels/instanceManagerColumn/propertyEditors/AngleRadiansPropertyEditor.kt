package com.pandulapeter.kubriko.editor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.editor.implementation.userInterface.components.EditorSlider
import com.pandulapeter.kubriko.engine.implementation.extensions.rad
import com.pandulapeter.kubriko.engine.types.AngleRadians

@Composable
internal fun AngleRadiansPropertyEditor(
    name: String,
    value: AngleRadians,
    onValueChanged: (AngleRadians) -> Unit,
) = EditorSlider(
    modifier = Modifier.padding(horizontal = 8.dp),
    title = name,
    value = value.normalized,
    onValueChange = { onValueChanged(it.rad) },
    valueRange = 0f..6.27f
)