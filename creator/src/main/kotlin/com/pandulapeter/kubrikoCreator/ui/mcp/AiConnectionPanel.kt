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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.mcp.McpServerState
import com.pandulapeter.kubrikoCreator.mcp.McpSettings
import com.pandulapeter.kubrikoCreator.mcp.McpToolCall
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClient
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClientSetup
import com.pandulapeter.kubrikoCreator.mcp.clients.AiClientStatus
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ai_connection_advanced
import kubriko.creator.generated.resources.ai_connection_calls
import kubriko.creator.generated.resources.ai_connection_calls_empty
import kubriko.creator.generated.resources.ai_connection_clients
import kubriko.creator.generated.resources.ai_connection_enabled
import kubriko.creator.generated.resources.ai_connection_note
import kubriko.creator.generated.resources.ai_connection_other
import kubriko.creator.generated.resources.ai_connection_state_failed
import kubriko.creator.generated.resources.ai_connection_state_off
import kubriko.creator.generated.resources.ai_connection_state_running
import kubriko.creator.generated.resources.ai_connection_state_starting
import kubriko.creator.generated.resources.ai_connection_title
import kubriko.creator.generated.resources.ic_ai
import kubriko.creator.generated.resources.ic_history
import org.jetbrains.compose.resources.stringResource

/**
 * The AI connection: AI tools such as Claude Code connected in one click, the address for any other one, the
 * settings few people need, and the tool calls they made.
 */
@Composable
internal fun AiConnectionPanel(
    settings: McpSettings,
    state: McpServerState,
    clientSetups: List<AiClientSetup>,
    clientStatuses: Map<String, AiClientStatus>,
    calls: List<McpToolCall>,
    onEnabledChange: (Boolean) -> Unit,
    onRefreshClients: () -> Unit,
    onConnect: (AiClientSetup) -> Unit,
    onDisconnect: (AiClientSetup) -> Unit,
    onPortChange: (Int) -> Boolean,
    onRegenerateToken: () -> Unit,
    onCommandChange: (AiClient, String) -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.verticalScroll(rememberScrollState()),
) {
    var isAdvancedOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { onRefreshClients() }
    PanelHeader(
        icon = Res.drawable.ic_ai,
        title = stringResource(Res.string.ai_connection_title),
    )
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(Res.string.ai_connection_clients),
            style = MaterialTheme.typography.titleSmall,
        )
        clientSetups.forEach { setup ->
            AiClientRow(
                setup = setup,
                status = clientStatuses[setup.id] ?: AiClientStatus.Checking,
                onConnect = { onConnect(setup) },
                onDisconnect = { onDisconnect(setup) },
            )
        }
        HorizontalDivider()
        EnabledRow(
            isEnabled = settings.isEnabled,
            onEnabledChange = onEnabledChange,
        )
        StateText(
            state = state,
            url = settings.url,
        )
        CopyableText(
            title = stringResource(Res.string.ai_connection_other),
            text = settings.urlWithToken,
        )
        Text(
            text = stringResource(Res.string.ai_connection_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { isAdvancedOpen = !isAdvancedOpen }) {
            Text(stringResource(Res.string.ai_connection_advanced))
        }
        if (isAdvancedOpen) {
            AiConnectionAdvanced(
                settings = settings,
                onPortChange = onPortChange,
                onRegenerateToken = onRegenerateToken,
                onCommandChange = onCommandChange,
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
    PanelHeader(
        icon = Res.drawable.ic_history,
        title = stringResource(Res.string.ai_connection_calls),
    )
    if (calls.isEmpty()) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp),
            text = stringResource(Res.string.ai_connection_calls_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    calls.asReversed().forEach { ToolCallRow(it) }
}

@Composable
private fun EnabledRow(
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(
        modifier = Modifier.weight(1f),
        text = stringResource(Res.string.ai_connection_enabled),
        style = MaterialTheme.typography.bodyMedium,
    )
    Switch(
        checked = isEnabled,
        onCheckedChange = onEnabledChange,
    )
}

@Composable
private fun StateText(
    state: McpServerState,
    url: String,
) = Text(
    text = when (state) {
        McpServerState.Off -> stringResource(Res.string.ai_connection_state_off)
        McpServerState.Starting -> stringResource(Res.string.ai_connection_state_starting)
        is McpServerState.Running -> stringResource(Res.string.ai_connection_state_running, url)
        is McpServerState.Failed -> stringResource(Res.string.ai_connection_state_failed, state.port.toString())
    },
    style = MaterialTheme.typography.bodySmall,
    color = when (state) {
        is McpServerState.Failed -> MaterialTheme.colorScheme.error
        is McpServerState.Running -> MaterialTheme.colorScheme.primary
        McpServerState.Off, McpServerState.Starting -> MaterialTheme.colorScheme.onSurfaceVariant
    },
)

@Composable
private fun ToolCallRow(
    call: McpToolCall,
) = Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
) {
    Text(
        text = call.toolName,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = FontFamily.Monospace,
        color = if (call.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    )
    Text(
        text = call.result,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}
