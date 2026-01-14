/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
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
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorIcon
import com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorNumberInput
import com.pandulapeter.kubriko.types.Scale
import kubriko.tools.scene_editor.generated.resources.Res
import kubriko.tools.scene_editor.generated.resources.ic_unit

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
        extraContent = {
            UnitButton(
                value = value.horizontal,
                onValueChanged = { onValueChanged(Scale(it, value.vertical)) },
            )
        }
    )
    EditorNumberInput(
        modifier = Modifier.weight(1f),
        name = "$name.vertical",
        value = value.vertical,
        onValueChanged = { onValueChanged(Scale(value.horizontal, it)) },
        extraContent = {
            UnitButton(
                value = value.vertical,
                onValueChanged = { onValueChanged(Scale(value.horizontal, it)) },
            )
        }
    )
}


@Composable
private fun UnitButton(
    value: Float,
    onValueChanged: (Float) -> Unit,
) {
    EditorIcon(
        drawableResource = Res.drawable.ic_unit,
        contentDescription = "Unit",
        isEnabled = value != 1f,
        onClick = { onValueChanged(1f) }
    )
}