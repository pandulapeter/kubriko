package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorNumberInput
import com.pandulapeter.kubriko.types.Scale

@Composable
internal fun ScalePropertyEditor(
    name: String,
    value: Scale,
    onValueChanged: (Scale) -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    EditorNumberInput(
        modifier = Modifier.weight(1f),
        name = "$name.horizontal",
        value = value.horizontal,
        onValueChanged = { onValueChanged(Scale(it, value.vertical)) },
    )
    EditorNumberInput(
        modifier = Modifier.weight(1f),
        name = "$name.vertical",
        value = value.vertical,
        onValueChanged = { onValueChanged(Scale(value.horizontal, it)) },
    )
}