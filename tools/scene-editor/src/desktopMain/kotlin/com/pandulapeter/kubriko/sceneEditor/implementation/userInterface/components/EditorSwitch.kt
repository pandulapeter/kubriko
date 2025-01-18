/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

@Composable
internal fun EditorSwitch(
    modifier: Modifier = Modifier,
    text: String,
    isChecked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
    isEnabled: Boolean = true,
) = Row(
    modifier = modifier.selectable(
        selected = isChecked,
        onClick = { onCheckedChanged(!isChecked) }
    ).padding(start = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
) {
    EditorText(
        modifier = Modifier.weight(1f),
        text = text,
    )
    Switch(
        modifier = Modifier.scale(0.6f).height(24.dp),
        checked = isChecked,
        onCheckedChange = onCheckedChanged,
        enabled = isEnabled,
    )
}
