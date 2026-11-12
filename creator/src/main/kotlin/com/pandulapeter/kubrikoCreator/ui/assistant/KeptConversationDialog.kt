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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ai.assistant.ConversationMatch
import com.pandulapeter.kubrikoCreator.ai.assistant.KeptMessage
import com.pandulapeter.kubrikoCreator.ui.components.CreatorDialog
import com.pandulapeter.kubrikoCreator.ui.components.flashBackground
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_close
import kubriko.creator.generated.resources.conversation_kept_notice
import kubriko.creator.generated.resources.conversation_search_agent
import kubriko.creator.generated.resources.conversation_search_you
import org.jetbrains.compose.resources.stringResource

/**
 * A conversation another agent holds, read from what Kubriko Creator kept of it ([readMessages]), since only that agent
 * can continue it: every message, scrolled to the one [match] found, which lights up.
 */
@Composable
internal fun KeptConversationDialog(
    match: ConversationMatch,
    readMessages: suspend () -> List<KeptMessage>,
    onDismiss: () -> Unit,
) = CreatorDialog(
    title = conversationTitle(match.conversation.title),
    maxWidth = 640.dp,
    onDismiss = onDismiss,
    confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_close)) } },
) {
    val messages by produceState<List<KeptMessage>?>(null, match) { value = readMessages() }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.conversation_kept_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        messages?.let { kept -> KeptMessageList(kept, match.messageIndex, flashKey = match) }
    }
}

@Composable
private fun KeptMessageList(
    messages: List<KeptMessage>,
    foundIndex: Int,
    flashKey: Any,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages, foundIndex) { if (foundIndex in messages.indices) listState.scrollToItem(foundIndex) }
    SelectionContainer {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(messages) { index, message ->
                Column(
                    modifier = Modifier.fillMaxWidth().flashBackground(flashKey.takeIf { index == foundIndex }).padding(4.dp),
                ) {
                    Text(
                        text = stringResource(if (message.isFromUser) Res.string.conversation_search_you else Res.string.conversation_search_agent),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(text = message.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
