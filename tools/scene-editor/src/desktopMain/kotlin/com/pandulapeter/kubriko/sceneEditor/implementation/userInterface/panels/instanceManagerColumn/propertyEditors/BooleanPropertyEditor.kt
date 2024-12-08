package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorSwitch

@Composable
internal fun BooleanPropertyEditor(
    modifier: Modifier = Modifier,
    name: String,
    value: Boolean,
    onValueChanged: (Boolean) -> Unit,
    enabled: Boolean = true,
) = EditorSwitch(
    modifier = modifier.fillMaxWidth(),
    text = name,
    isEnabled = enabled,
    isChecked = value,
    onCheckedChanged = onValueChanged,
)