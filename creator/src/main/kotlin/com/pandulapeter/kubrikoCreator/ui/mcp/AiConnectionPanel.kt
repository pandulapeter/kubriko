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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.mcp.McpServerState
import com.pandulapeter.kubrikoCreator.mcp.McpSettings
import com.pandulapeter.kubrikoCreator.mcp.McpToolCall
import com.pandulapeter.kubrikoCreator.mcp.claudeCodeCommand
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.PanelHeader
import com.pandulapeter.kubrikoCreator.ui.components.copyToClipboard
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_apply_port
import kubriko.creator.generated.resources.action_copy
import kubriko.creator.generated.resources.action_regenerate_token
import kubriko.creator.generated.resources.ai_connection_calls
import kubriko.creator.generated.resources.ai_connection_calls_empty
import kubriko.creator.generated.resources.ai_connection_claude_code
import kubriko.creator.generated.resources.ai_connection_enabled
import kubriko.creator.generated.resources.ai_connection_note
import kubriko.creator.generated.resources.ai_connection_port
import kubriko.creator.generated.resources.ai_connection_port_invalid
import kubriko.creator.generated.resources.ai_connection_state_failed
import kubriko.creator.generated.resources.ai_connection_state_off
import kubriko.creator.generated.resources.ai_connection_state_running
import kubriko.creator.generated.resources.ai_connection_state_starting
import kubriko.creator.generated.resources.ai_connection_title
import kubriko.creator.generated.resources.ai_connection_token
import kubriko.creator.generated.resources.ic_ai
import kubriko.creator.generated.resources.ic_check
import kubriko.creator.generated.resources.ic_copy
import kubriko.creator.generated.resources.ic_history
import kubriko.creator.generated.resources.ic_refresh
import org.jetbrains.compose.resources.stringResource

/**
 * The AI connection: whether AI clients such as Claude Code can use Kubriko Creator's tools, how to connect them, and
 * the tool calls they made.
 */
@Composable
internal fun AiConnectionPanel(
    settings: McpSettings,
    state: McpServerState,
    calls: List<McpToolCall>,
    onEnabledChange: (Boolean) -> Unit,
    onPortChange: (Int) -> Boolean,
    onRegenerateToken: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier.verticalScroll(rememberScrollState()),
) {
    PanelHeader(
        icon = Res.drawable.ic_ai,
        title = stringResource(Res.string.ai_connection_title),
    )
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EnabledRow(
            isEnabled = settings.isEnabled,
            onEnabledChange = onEnabledChange,
        )
        StateText(
            state = state,
            url = settings.url,
        )
        PortField(
            port = settings.port,
            onPortChange = onPortChange,
        )
        TokenRow(
            token = settings.token,
            onRegenerateToken = onRegenerateToken,
        )
        CopyableText(
            title = stringResource(Res.string.ai_connection_claude_code),
            text = settings.claudeCodeCommand(),
        )
        Text(
            text = stringResource(Res.string.ai_connection_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
private fun CopyableText(
    title: String,
    text: String,
) = Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = MaterialTheme.typography.bodyMedium,
        )
        IconActionButton(
            icon = Res.drawable.ic_copy,
            label = Res.string.action_copy,
            onClick = { copyToClipboard(text) },
        )
    }
    SelectionContainer {
        Text(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.small).padding(8.dp),
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}

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
