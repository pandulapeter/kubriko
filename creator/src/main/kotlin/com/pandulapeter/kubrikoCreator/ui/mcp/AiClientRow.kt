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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClientStatus
import com.pandulapeter.kubrikoCreator.ui.components.openInBrowser
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_connect
import kubriko.creator.generated.resources.action_disconnect
import kubriko.creator.generated.resources.action_install
import kubriko.creator.generated.resources.ai_client_checking
import kubriko.creator.generated.resources.ai_client_connected
import kubriko.creator.generated.resources.ai_client_connected_after_restart
import kubriko.creator.generated.resources.ai_client_failed
import kubriko.creator.generated.resources.ai_client_needs_program
import kubriko.creator.generated.resources.ai_client_not_connected
import kubriko.creator.generated.resources.ai_client_not_installed
import org.jetbrains.compose.resources.stringResource

/**
 * One setup of an AI tool: whether it is installed and connected, and the button that changes that.
 *
 * @param title The setup's name.
 * @param clientName The tool's name.
 * @param isReadOnStart Whether the tool only reads its servers when it starts.
 */
@Composable
internal fun AiClientRow(
    title: String,
    clientName: String,
    installUrl: String,
    isReadOnStart: Boolean,
    status: AiClientStatus,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Column(
        modifier = Modifier.weight(1f),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = when (status) {
                AiClientStatus.Checking -> stringResource(Res.string.ai_client_checking)
                AiClientStatus.NotInstalled -> stringResource(Res.string.ai_client_not_installed)
                AiClientStatus.NotConnected -> stringResource(Res.string.ai_client_not_connected)
                is AiClientStatus.NeedsProgram -> stringResource(Res.string.ai_client_needs_program, status.program.name)
                AiClientStatus.Connected -> stringResource(if (isReadOnStart) Res.string.ai_client_connected_after_restart else Res.string.ai_client_connected, clientName)
                is AiClientStatus.Failed -> stringResource(Res.string.ai_client_failed, status.output)
            },
            style = MaterialTheme.typography.bodySmall,
            color = when (status) {
                AiClientStatus.Connected -> MaterialTheme.colorScheme.primary
                is AiClientStatus.Failed -> MaterialTheme.colorScheme.error
                AiClientStatus.Checking, AiClientStatus.NotInstalled, is AiClientStatus.NeedsProgram, AiClientStatus.NotConnected -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
    when (status) {
        AiClientStatus.Checking -> CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
        )
        AiClientStatus.NotInstalled -> OutlinedButton(onClick = { openInBrowser(installUrl) }) {
            Text(stringResource(Res.string.action_install))
        }
        is AiClientStatus.NeedsProgram -> OutlinedButton(onClick = { openInBrowser(status.program.installUrl) }) {
            Text(stringResource(Res.string.action_install))
        }
        AiClientStatus.Connected -> OutlinedButton(onClick = onDisconnect) {
            Text(stringResource(Res.string.action_disconnect))
        }
        AiClientStatus.NotConnected, is AiClientStatus.Failed -> FilledTonalButton(onClick = onConnect) {
            Text(stringResource(Res.string.action_connect))
        }
    }
}
