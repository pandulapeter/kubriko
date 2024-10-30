package com.pandulapeter.gameTemplate.editor.implementation.userInterface.propertyEditors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.gameTemplate.editor.implementation.userInterface.components.EditorTextLabel
import com.pandulapeter.gameTemplate.engine.types.WorldCoordinates

@Composable
internal fun WorldCoordinatesPropertyEditor(
    name: String,
    value: WorldCoordinates,
    onValueChanged: (WorldCoordinates) -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
) {
    EditorTextLabel(
        text = "$name.x: ${value.x}",
    )
    EditorTextLabel(
        text = "$name.y: ${value.y}",
    )
}