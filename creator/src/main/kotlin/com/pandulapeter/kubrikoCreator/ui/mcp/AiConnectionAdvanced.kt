/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.mcp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.mcp.McpSettings
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClient
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.copyToClipboard
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_apply_command
import kubriko.creator.generated.resources.action_apply_port
import kubriko.creator.generated.resources.action_copy
import kubriko.creator.generated.resources.action_regenerate_token
import kubriko.creator.generated.resources.ai_client_command
import kubriko.creator.generated.resources.ai_connection_port
import kubriko.creator.generated.resources.ai_connection_port_invalid
import kubriko.creator.generated.resources.ai_connection_token
import kubriko.creator.generated.resources.ic_check
import kubriko.creator.generated.resources.ic_copy
import kubriko.creator.generated.resources.ic_refresh
import org.jetbrains.compose.resources.stringResource

/**
 * What most users never need to change: the port, the token, and the commands that run each AI tool.
 */
@Composable
internal fun AiConnectionAdvanced(
    settings: McpSettings,
    onPortChange: (Int) -> Boolean,
    onRegenerateToken: () -> Unit,
    onCommandChange: (AiClient, String) -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(10.dp),
) {
    PortField(
        port = settings.port,
        onPortChange = onPortChange,
    )
    TokenRow(
        token = settings.token,
        onRegenerateToken = onRegenerateToken,
    )
    AiClient.entries.forEach { client ->
        CommandField(
            client = client,
            command = settings.commandOf(client),
            onCommandChange = { onCommandChange(client, it) },
        )
    }
}

@Composable
private fun PortField(
    port: Int,
    onPortChange: (Int) -> Boolean,
) = Column {
    var text by remember(port) { mutableStateOf(port.toString()) }
    var isInvalid by remember(port) { mutableStateOf(false) }
    val apply = { isInvalid = text.toIntOrNull()?.let(onPortChange) != true }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.ai_connection_port),
            style = MaterialTheme.typography.bodyMedium,
        )
        CompactTextField(
            modifier = Modifier.width(96.dp).onPreviewKeyEvent { event ->
                (event.type == KeyEventType.KeyDown && event.key == Key.Enter).also { if (it) apply() }
            },
            value = text,
            onValueChange = { value ->
                text = value.filter(Char::isDigit).take(5)
                isInvalid = false
            },
            isError = isInvalid,
        )
        IconActionButton(
            icon = Res.drawable.ic_check,
            label = Res.string.action_apply_port,
            onClick = apply,
            isEnabled = text != port.toString(),
        )
    }
    if (isInvalid) {
        Text(
            text = stringResource(Res.string.ai_connection_port_invalid, McpSettings.portRange.first.toString(), McpSettings.portRange.last.toString()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun TokenRow(
    token: String,
    onRegenerateToken: () -> Unit,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(
        modifier = Modifier.weight(1f),
        text = stringResource(Res.string.ai_connection_token),
        style = MaterialTheme.typography.bodyMedium,
    )
    IconActionButton(
        icon = Res.drawable.ic_copy,
        label = Res.string.action_copy,
        onClick = { copyToClipboard(token) },
    )
    IconActionButton(
        icon = Res.drawable.ic_refresh,
        label = Res.string.action_regenerate_token,
        onClick = onRegenerateToken,
    )
}

@Composable
private fun CommandField(
    client: AiClient,
    command: String,
    onCommandChange: (String) -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    var text by remember(command) { mutableStateOf(command) }
    Text(
        text = stringResource(Res.string.ai_client_command, client.displayName),
        style = MaterialTheme.typography.bodyMedium,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactTextField(
            modifier = Modifier.weight(1f).onPreviewKeyEvent { event ->
                (event.type == KeyEventType.KeyDown && event.key == Key.Enter).also { if (it) onCommandChange(text) }
            },
            value = text,
            onValueChange = { text = it },
            placeholder = client.defaultCommand,
        )
        IconActionButton(
            icon = Res.drawable.ic_check,
            label = Res.string.action_apply_command,
            onClick = { onCommandChange(text) },
            isEnabled = text != command,
        )
    }
}
