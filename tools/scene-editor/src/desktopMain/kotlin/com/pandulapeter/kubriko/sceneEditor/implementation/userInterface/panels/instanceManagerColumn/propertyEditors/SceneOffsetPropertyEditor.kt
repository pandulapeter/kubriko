package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorIcon
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorNumberInput
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.ScenePixel
import kubriko.tools.scene_editor.generated.resources.Res
import kubriko.tools.scene_editor.generated.resources.ic_center

@Composable
internal fun SceneOffsetPropertyEditor(
    name: String,
    value: SceneOffset,
    onValueChanged: (SceneOffset) -> Unit,
    xValueRange: ClosedFloatingPointRange<Float>? = null,
    yValueRange: ClosedFloatingPointRange<Float>? = null,
    shouldShowCenterButton: Boolean = false,
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
        extraContent = {
            if (shouldShowCenterButton) {
                CenterButton(
                    value = value.x,
                    onValueChanged = { onValueChanged(SceneOffset(it, value.y)) },
                    range = xValueRange,
                )
            }
        }
    )
    EditorNumberInput(
        modifier = Modifier.weight(1f),
        name = "$name.y",
        value = value.y.raw,
        onValueChanged = { onValueChanged(SceneOffset(value.x, it.scenePixel)) },
        valueRange = yValueRange,
        extraContent = {
            if (shouldShowCenterButton) {
                CenterButton(
                    value = value.y,
                    onValueChanged = { onValueChanged(SceneOffset(value.x, it)) },
                    range = yValueRange,
                )
            }
        }
    )
}

@Composable
private fun CenterButton(
    value: ScenePixel,
    onValueChanged: (ScenePixel) -> Unit,
    range: ClosedFloatingPointRange<Float>?,
) {
    if (range != null) {
        val center = (range.endInclusive - range.start).scenePixel / 2
        EditorIcon(
            drawableResource = Res.drawable.ic_center,
            contentDescription = "Center",
            isEnabled = value != center,
            onClick = { onValueChanged(center) }
        )
    }
}