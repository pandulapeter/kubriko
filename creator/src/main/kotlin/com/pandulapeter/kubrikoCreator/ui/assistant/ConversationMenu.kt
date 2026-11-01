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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantChat
import com.pandulapeter.kubrikoCreator.ai.assistant.SavedConversation
import com.pandulapeter.kubrikoCreator.ui.components.MenuButton
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_new_conversation
import kubriko.creator.generated.resources.assistant_conversation_earlier
import org.jetbrains.compose.resources.stringResource

/**
 * The conversation shown, as a menu of the ones open and the earlier ones that can be continued, while there is more
 * than the one shown.
 */
@Composable
internal fun ConversationMenu(
    chats: List<AssistantChat>,
    current: AssistantChat?,
    savedConversations: List<SavedConversation>,
    onSelected: (AssistantChat) -> Unit,
    onResume: (SavedConversation) -> Unit,
) {
    if (current == null || (chats.size < 2 && savedConversations.isEmpty())) return
    Box(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        MenuButton(
            label = current.title(),
            options = chats.asReversed().map(ConversationMenuItem::Open) + savedConversations.map(ConversationMenuItem::Earlier),
            optionText = { item ->
                when (item) {
                    is ConversationMenuItem.Open -> item.chat.title()
                    is ConversationMenuItem.Earlier -> stringResource(Res.string.assistant_conversation_earlier, conversationTitle(item.conversation.title))
                }
            },
            onSelected = { item ->
                when (item) {
                    is ConversationMenuItem.Open -> onSelected(item.chat)
                    is ConversationMenuItem.Earlier -> onResume(item.conversation)
                }
            },
        )
    }
}

@Composable
private fun AssistantChat.title() = transcript.collectAsState().value.title?.let(::conversationTitle)
    ?: stringResource(Res.string.action_new_conversation)
