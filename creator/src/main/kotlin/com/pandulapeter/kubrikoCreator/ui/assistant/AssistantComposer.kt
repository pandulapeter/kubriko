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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agentclientprotocol.model.AvailableCommand
import com.pandulapeter.kubrikoCreator.ui.components.CompactTextField
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_send
import kubriko.creator.generated.resources.action_stop
import kubriko.creator.generated.resources.assistant_input_hint
import kubriko.creator.generated.resources.ic_send
import kubriko.creator.generated.resources.ic_stop
import org.jetbrains.compose.resources.stringResource

/**
 * Where the user writes to the assistant: Enter sends, Shift+Enter starts a new line, and a `/` at the start offers
 * the agent's slash commands (Tab takes the first).
 *
 * @param isEnabled Whether the conversation takes prompts.
 * @param isBusy Whether the agent works on a turn, which Stop ends.
 */
@Composable
internal fun AssistantComposer(
    commands: List<AvailableCommand>,
    isEnabled: Boolean,
    isBusy: Boolean,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier,
) {
    var value by remember { mutableStateOf(TextFieldValue()) }
    val text = value.text
    val completions = remember(text, commands) { slashCompletions(text, commands) }
    fun send() {
        if (!isEnabled || isBusy || text.isBlank()) return
        onSend(text.trim())
        value = TextFieldValue()
    }
    fun complete(command: AvailableCommand) {
        val completed = "/${command.name} "
        value = TextFieldValue(completed, TextRange(completed.length))
    }
    completions.forEach { command ->
        SlashCommandRow(
            command = command,
            onClick = { complete(command) },
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        CompactTextField(
            modifier = Modifier
                .weight(1f)
                .heightIn(max = 160.dp)
                .onPreviewKeyEvent { event ->
                    when {
                        event.type != KeyEventType.KeyDown -> false
                        event.key == Key.Enter && !event.isShiftPressed -> true.also { send() }
                        event.key == Key.Tab && completions.isNotEmpty() -> true.also { complete(completions.first()) }
                        else -> false
                    }
                },
            value = value,
            onValueChange = { value = it },
            placeholder = stringResource(Res.string.assistant_input_hint),
            isSingleLine = false,
        )
        if (isBusy) {
            IconActionButton(
                icon = Res.drawable.ic_stop,
                label = Res.string.action_stop,
                onClick = onStop,
            )
        } else {
            IconActionButton(
                icon = Res.drawable.ic_send,
                label = Res.string.action_send,
                onClick = ::send,
                isEnabled = isEnabled && text.isNotBlank(),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SlashCommandRow(
    command: AvailableCommand,
    onClick: () -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 4.dp),
) {
    Text(
        text = "/${command.name}",
        style = MaterialTheme.typography.labelLarge,
    )
    Text(
        modifier = Modifier.padding(start = 8.dp),
        text = command.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * The [commands] whose names start with what follows a `/` that begins [text], while no space follows it yet, at
 * most [MAXIMUM_COMPLETIONS] of them.
 */
internal fun slashCompletions(text: String, commands: List<AvailableCommand>): List<AvailableCommand> {
    if (!text.startsWith("/") || text.any { it.isWhitespace() }) return emptyList()
    val prefix = text.removePrefix("/")
    return commands.filter { it.name.startsWith(prefix, ignoreCase = true) && it.name != prefix }.take(MAXIMUM_COMPLETIONS)
}

private const val MAXIMUM_COMPLETIONS = 6
