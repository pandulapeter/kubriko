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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.CreatorDialog
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_cancel
import kubriko.creator.generated.resources.action_delete
import kubriko.creator.generated.resources.action_delete_conversation
import kubriko.creator.generated.resources.action_search_conversations
import kubriko.creator.generated.resources.assistant_delete_conversation_message
import kubriko.creator.generated.resources.assistant_delete_conversation_title
import kubriko.creator.generated.resources.ic_delete
import kubriko.creator.generated.resources.ic_search
import org.jetbrains.compose.resources.stringResource

/**
 * The line above a conversation: its [conversation] title, which takes all the room the panel has, searching the
 * project's conversations ([onSearch]), and deleting it once something has been said in it ([canDelete]), asked first,
 * since a deleted conversation can't be continued.
 */
@Composable
internal fun ConversationBar(
    canDelete: Boolean,
    onDelete: () -> Unit,
    onSearch: () -> Unit,
    conversation: @Composable (Modifier) -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    conversation(Modifier.weight(1f))
    IconActionButton(
        icon = Res.drawable.ic_search,
        label = Res.string.action_search_conversations,
        onClick = onSearch,
    )
    if (canDelete) {
        var isConfirming by remember { mutableStateOf(false) }
        IconActionButton(
            icon = Res.drawable.ic_delete,
            label = Res.string.action_delete_conversation,
            onClick = { isConfirming = true },
        )
        if (isConfirming) {
            DeleteConfirmation(
                onConfirm = {
                    isConfirming = false
                    onDelete()
                },
                onDismiss = { isConfirming = false },
            )
        }
    }
}

@Composable
private fun DeleteConfirmation(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) = CreatorDialog(
    title = stringResource(Res.string.assistant_delete_conversation_title),
    maxWidth = 440.dp,
    onDismiss = onDismiss,
    confirmButton = {
        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Text(stringResource(Res.string.action_delete))
        }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
) {
    Text(stringResource(Res.string.assistant_delete_conversation_message))
}
