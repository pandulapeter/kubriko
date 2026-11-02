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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A section of a settings dialog: its [title], an optional [description], and its [content].
 */
@Composable
internal fun SettingsSection(
    title: StringResource,
    description: StringResource? = null,
    content: @Composable () -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    if (description != null) {
        Text(
            text = stringResource(description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    content()
}
