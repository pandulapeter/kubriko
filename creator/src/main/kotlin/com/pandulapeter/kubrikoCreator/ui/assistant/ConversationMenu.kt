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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ai.assistant.AssistantChat
import com.pandulapeter.kubrikoCreator.ai.assistant.SavedConversation
import com.pandulapeter.kubrikoCreator.ui.components.ContextAction
import com.pandulapeter.kubrikoCreator.ui.components.RightClickMenu
import com.pandulapeter.kubrikoCreator.ui.components.rememberMenuAnchor
import com.pandulapeter.kubrikoCreator.ui.components.rememberRightClickMenuState
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_new_conversation
import kubriko.creator.generated.resources.assistant_conversation_earlier
import kubriko.creator.generated.resources.ic_chevron
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The current conversation's title, as plain text while it is the only one, and opening the list of open and earlier
 * conversations once there are others, the one talked in last first.
 */
@Composable
internal fun ConversationMenu(
    chats: List<AssistantChat>,
    current: AssistantChat?,
    savedConversations: List<SavedConversation>,
    onSelected: (AssistantChat) -> Unit,
    onResume: (SavedConversation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasOthers = current != null && (chats.size > 1 || savedConversations.isNotEmpty())
    val menu = rememberRightClickMenuState()
    val anchor = rememberMenuAnchor(menu)
    Box(modifier) {
        Row(
            modifier = Modifier
                .heightIn(min = 32.dp)
                .clip(MaterialTheme.shapes.small)
                .then(anchor.modifier)
                .then(if (hasOthers) Modifier.clickable(onClick = anchor::open) else Modifier)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f, fill = false),
                text = current?.title() ?: stringResource(Res.string.action_new_conversation),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (hasOthers) {
                Icon(
                    modifier = Modifier.size(12.dp),
                    painter = painterResource(Res.drawable.ic_chevron),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val open = chats.map { chat -> key(chat) { ConversationChoice.Open(chat, chat.lastMessageAt.collectAsState().value) } }
        RightClickMenu(
            state = menu,
            actions = conversationChoices(open, savedConversations).map { choice ->
                when (choice) {
                    is ConversationChoice.Open -> ContextAction(label = choice.chat.title(), isChecked = choice.chat == current) { onSelected(choice.chat) }
                    is ConversationChoice.Earlier -> ContextAction(
                        label = stringResource(Res.string.assistant_conversation_earlier, conversationTitle(choice.conversation.title)),
                        isChecked = false,
                    ) { onResume(choice.conversation) }
                }
            },
        )
    }
}

/**
 * A conversation the menu lists: one open, or an earlier one that can be continued.
 */
internal sealed interface ConversationChoice {
    /**
     * When the user or the agent last said something in it, in milliseconds since the epoch.
     */
    val lastMessageAt: Long

    data class Open(val chat: AssistantChat, override val lastMessageAt: Long) : ConversationChoice

    data class Earlier(val conversation: SavedConversation) : ConversationChoice {
        override val lastMessageAt: Long get() = conversation.updatedAt
    }
}

/**
 * The [open] and [earlier] conversations in one list, the one talked in last first (an open one before an earlier one
 * talked in at the same moment).
 */
internal fun conversationChoices(open: List<ConversationChoice.Open>, earlier: List<SavedConversation>): List<ConversationChoice> =
    (open + earlier.map(ConversationChoice::Earlier)).sortedByDescending { it.lastMessageAt }

@Composable
private fun AssistantChat.title() = transcript.collectAsState().value.title?.let(::conversationTitle)
    ?: stringResource(Res.string.action_new_conversation)
