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

import com.pandulapeter.kubrikoCreator.foundation.TextSnippet

/**
 * Finds a query, ignoring case, in what the user asked and the agent answered in saved conversations ([KeptMessage]),
 * in memory.
 */
internal object ConversationSearch {

    /**
     * Every message of [conversations] (each with its kept messages, newest conversation first) holding [query], at most
     * [limit], the newest conversation's first and each conversation's in order, with the words around the first line
     * that holds it. Those of agents other than [agentId] are marked as such.
     */
    fun search(query: String, conversations: List<Pair<SavedConversation, List<KeptMessage>>>, agentId: String, limit: Int): List<ConversationMatch> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val matches = ArrayList<ConversationMatch>()
        for ((conversation, messages) in conversations) {
            val found = messages.mapIndexedNotNull { index, message -> message.text.lineSequence().firstNotNullOfOrNull { TextSnippet.of(it, trimmed) }?.let { index to it } }
            found.forEachIndexed { occurrence, (index, snippet) ->
                matches += ConversationMatch(conversation, trimmed, index, found.size - 1 - occurrence, messages[index].isFromUser, conversation.agentId != agentId, snippet)
                if (matches.size == limit) return matches
            }
        }
        return matches
    }
}
