/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What the user asked or the agent answered in a saved conversation, kept as plain text so the conversation can be
 * searched, and read where its agent can't continue it ([ConversationTextStore]).
 *
 * @param isFromUser Whether the user wrote it, rather than the agent.
 */
@Serializable
public data class KeptMessage(
    @SerialName("isFromUser") val isFromUser: Boolean,
    @SerialName("text") val text: String,
) {
    public companion object {

        /**
         * Whether [item] is a message that was kept as [message]: what the user asked or the agent answered, with the
         * same text as far as it was kept.
         */
        public fun isKeptAs(item: TranscriptItem, message: KeptMessage): Boolean = when (item) {
            is TranscriptItem.UserMessage -> message.isFromUser && item.text.take(MESSAGE_LENGTH) == message.text
            is TranscriptItem.AgentMessage -> !message.isFromUser && item.text.take(MESSAGE_LENGTH) == message.text
            else -> false
        }

        /**
         * The messages of [items] kept for searching: what the user asked and the agent answered, without tool calls,
         * thoughts, or anything else, each at most [MESSAGE_LENGTH] characters, and only the newest that fit in
         * [TOTAL_LENGTH] characters together, so a long conversation keeps a bounded file.
         */
        public fun of(items: List<TranscriptItem>): List<KeptMessage> {
            val messages = items.mapNotNull { item ->
                when (item) {
                    is TranscriptItem.UserMessage -> KeptMessage(isFromUser = true, text = item.text.take(MESSAGE_LENGTH))
                    is TranscriptItem.AgentMessage -> KeptMessage(isFromUser = false, text = item.text.take(MESSAGE_LENGTH))
                    else -> null
                }
            }.filter { it.text.isNotBlank() }
            var total = 0
            return messages.asReversed().takeWhile { message -> (total + message.text.length <= TOTAL_LENGTH).also { total += message.text.length } }.asReversed()
        }

        /**
         * How long a kept message is at most.
         */
        public const val MESSAGE_LENGTH: Int = 8_000

        /**
         * How long a conversation's kept messages are at most together.
         */
        public const val TOTAL_LENGTH: Int = 200_000
    }
}
