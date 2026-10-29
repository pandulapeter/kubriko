/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.openInBrowser
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_cancel
import kubriko.creator.generated.resources.action_get_api_key
import kubriko.creator.generated.resources.action_save
import kubriko.creator.generated.resources.assistant_api_key_failed
import kubriko.creator.generated.resources.assistant_api_key_hint
import kubriko.creator.generated.resources.assistant_api_key_note
import org.jetbrains.compose.resources.stringResource

/**
 * Asks for an Anthropic API key, which is kept in the operating system's secret store and billed for every conversation
 * from then on.
 *
 * @param hasFailed Whether the last key given couldn't be kept.
 */
@Composable
internal fun ApiKeyForm(
    hasFailed: Boolean,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier
        .fillMaxWidth()
        .padding(8.dp)
        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
        .padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    var key by remember { mutableStateOf("") }
    val save = { if (key.isNotBlank()) onSave(key) }
    Text(
        text = stringResource(Res.string.assistant_api_key_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    CompactTextField(
        modifier = Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event -> (event.key == Key.Enter || event.key == Key.NumPadEnter).also { if (it) save() } },
        value = key,
        onValueChange = { key = it },
        placeholder = stringResource(Res.string.assistant_api_key_hint),
        isError = hasFailed,
        isSecret = true,
    )
    if (hasFailed) {
        Text(
            text = stringResource(Res.string.assistant_api_key_failed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = save, enabled = key.isNotBlank()) {
            Text(stringResource(Res.string.action_save))
        }
        TextButton(onClick = onCancel) {
            Text(stringResource(Res.string.action_cancel))
        }
        TextButton(onClick = { openInBrowser(API_KEYS_URL) }) {
            Text(stringResource(Res.string.action_get_api_key))
        }
    }
}

private const val API_KEYS_URL = "https://platform.claude.com/settings/keys"
