package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorNumberInput
import com.pandulapeter.kubriko.types.SceneSize

@Composable
internal fun SceneSizePropertyEditor(
    name: String,
    value: SceneSize,
    onValueChanged: (SceneSize) -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    EditorNumberInput(
        modifier = Modifier.weight(1f),
        name = "$name.width",
        value = value.width.raw,
        onValueChanged = { onValueChanged(SceneSize(it.sceneUnit, value.height)) },
    )
    EditorNumberInput(
        modifier = Modifier.weight(1f),
        name = "$name.height",
        value = value.height.raw,
        onValueChanged = { onValueChanged(SceneSize(value.width, it.sceneUnit)) },
    )
}