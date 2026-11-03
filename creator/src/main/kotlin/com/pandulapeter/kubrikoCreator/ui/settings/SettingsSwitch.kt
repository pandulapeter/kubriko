/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.settings

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role

/**
 * A [SettingsRow] that is turned on and off as a whole, its switch only showing the state, so the label is as easy to
 * click as the switch and the press feedback spans the row.
 */
@Composable
internal fun SettingsSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
    descriptionColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    isEnabled: Boolean = true,
) = SettingsRow(
    modifier = Modifier.toggleable(value = checked, enabled = isEnabled, role = Role.Switch, onValueChange = onCheckedChange),
    label = label,
    description = description,
    descriptionColor = descriptionColor,
) {
    Switch(checked = checked, onCheckedChange = null, enabled = isEnabled)
}
