/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorIcon
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorNumberInput
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
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
        onValueChanged = { onValueChanged(SceneOffset(it.sceneUnit, value.y)) },
        valueRange = xValueRange,
        extraContent = {
            if (shouldShowCenterButton) {
                CenterButton(
                    value = value.x,
                    onValueChanged = { onValueChanged(SceneOffset(it, value.y)) },
                    range = xValueRange,
                    isHorizontal = true,
                )
            }
        }
    )
    EditorNumberInput(
        modifier = Modifier.weight(1f),
        name = "$name.y",
        value = value.y.raw,
        onValueChanged = { onValueChanged(SceneOffset(value.x, it.sceneUnit)) },
        valueRange = yValueRange,
        extraContent = {
            if (shouldShowCenterButton) {
                CenterButton(
                    value = value.y,
                    onValueChanged = { onValueChanged(SceneOffset(value.x, it)) },
                    range = yValueRange,
                    isHorizontal = false,
                )
            }
        }
    )
}

@Composable
private fun CenterButton(
    value: SceneUnit,
    onValueChanged: (SceneUnit) -> Unit,
    range: ClosedFloatingPointRange<Float>?,
    isHorizontal: Boolean,
) {
    if (range != null) {
        val center = (range.endInclusive - range.start).sceneUnit / 2
        EditorIcon(
            modifier = if (isHorizontal) Modifier else Modifier.rotate(90f),
            drawableResource = Res.drawable.ic_center,
            contentDescription = "Center",
            isEnabled = value != center,
            onClick = { onValueChanged(center) }
        )
    }
}