package com.pandulapeter.kubriko.editor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.editor.implementation.userInterface.components.EditorSlider
import com.pandulapeter.kubriko.engine.types.Scale

@Composable
internal fun ScalePropertyEditor(
    name: String,
    value: Scale,
    onValueChanged: (Scale) -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
) {
    EditorSlider(
        title = "$name.horizontal",
        value = value.horizontal,
        onValueChange = { onValueChanged(Scale(it, value.vertical)) },
        valueRange = 0f..10f
    )
    EditorSlider(
        title = "$name.vertical",
        value = value.vertical,
        onValueChange = { onValueChanged(Scale(value.horizontal, it)) },
        valueRange = 0f..10f
    )
}