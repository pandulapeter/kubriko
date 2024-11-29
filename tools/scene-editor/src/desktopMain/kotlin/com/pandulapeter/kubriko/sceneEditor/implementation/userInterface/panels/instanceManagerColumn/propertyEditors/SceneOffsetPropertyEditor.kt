package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorNumberInput
import com.pandulapeter.kubriko.types.SceneOffset

@Composable
internal fun SceneOffsetPropertyEditor(
    name: String,
    value: SceneOffset,
    onValueChanged: (SceneOffset) -> Unit,
    xValueRange: ClosedFloatingPointRange<Float>? = null,
    yValueRange: ClosedFloatingPointRange<Float>? = null,
) = Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    EditorNumberInput(
        modifier = Modifier.weight(1f),
        name = "$name.x",
        value = value.x.raw,
        onValueChanged = { onValueChanged(SceneOffset(it.scenePixel, value.y)) },
        valueRange = xValueRange,
    )
    EditorNumberInput(
        modifier = Modifier.weight(1f),
        name = "$name.y",
        value = value.y.raw,
        onValueChanged = { onValueChanged(SceneOffset(value.x, it.scenePixel)) },
        valueRange = yValueRange,
    )
}