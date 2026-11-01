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
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_cancel
import kubriko.creator.generated.resources.action_save
import kubriko.creator.generated.resources.assistant_custom_agent_hint
import kubriko.creator.generated.resources.assistant_custom_agent_note
import org.jetbrains.compose.resources.stringResource

/**
 * Asks for the command line of a custom agent, starting from [initialCommand].
 */
@Composable
internal fun CustomAgentForm(
    initialCommand: String?,
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
    var command by remember { mutableStateOf(initialCommand.orEmpty()) }
    val save = { if (command.isNotBlank()) onSave(command.trim()) }
    Text(
        text = stringResource(Res.string.assistant_custom_agent_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    CompactTextField(
        modifier = Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event -> (event.key == Key.Enter || event.key == Key.NumPadEnter).also { if (it) save() } },
        value = command,
        onValueChange = { command = it },
        placeholder = stringResource(Res.string.assistant_custom_agent_hint),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = save, enabled = command.isNotBlank()) {
            Text(stringResource(Res.string.action_save))
        }
        TextButton(onClick = onCancel) {
            Text(stringResource(Res.string.action_cancel))
        }
    }
}
