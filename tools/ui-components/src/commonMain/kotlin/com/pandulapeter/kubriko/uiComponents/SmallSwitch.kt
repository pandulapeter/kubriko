/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.uiComponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

@Composable
fun SmallSwitch(
    modifier: Modifier = Modifier,
    title: String,
    isEnabled: Boolean = true,
    isChecked: Boolean,
    onCheckedChanged: () -> Unit,
) = Row(
    modifier = modifier
        .fillMaxSize()
        .selectable(
            enabled = isEnabled,
            selected = isChecked,
            onClick = onCheckedChanged,
        )
        .padding(vertical = 4.dp)
        .padding(
            start = 16.dp,
            end = 8.dp,
        ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
) {
    Text(
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.labelSmall,
        text = title,
    )
    Switch(
        modifier = Modifier.scale(0.6f).height(24.dp),
        enabled = isEnabled,
        checked = isChecked,
        onCheckedChange = { onCheckedChanged() },
    )
}