package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorNumberInput
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.types.ScenePixel

@Composable
internal fun ScenePixelPropertyEditor(
    name: String,
    value: ScenePixel,
    onValueChanged: (ScenePixel) -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
) {
    EditorNumberInput(
        name = name,
        value = value.raw,
        onValueChanged = { onValueChanged(it.scenePixel) },
    )
}