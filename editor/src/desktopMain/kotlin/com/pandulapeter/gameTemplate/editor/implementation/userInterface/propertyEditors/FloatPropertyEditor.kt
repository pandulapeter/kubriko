package com.pandulapeter.gameTemplate.editor.implementation.userInterface.propertyEditors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorNumberInput

@Composable
internal fun FloatPropertyEditor(
    name: String,
    value: Float,
    onValueChanged: (Float) -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
) {
    EditorNumberInput(
        title = name,
        value = value,
        onValueChanged = { onValueChanged(it) },
    )
}