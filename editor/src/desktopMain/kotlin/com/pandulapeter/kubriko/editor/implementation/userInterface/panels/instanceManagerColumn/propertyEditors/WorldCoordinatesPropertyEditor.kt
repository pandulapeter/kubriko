package com.pandulapeter.kubriko.editor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.editor.implementation.userInterface.components.EditorTextLabel
import com.pandulapeter.kubriko.engine.types.WorldCoordinates

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