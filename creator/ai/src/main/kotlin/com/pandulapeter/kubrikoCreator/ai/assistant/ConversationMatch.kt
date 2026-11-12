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
 * A message of a saved conversation that holds what was searched for ([ConversationSearch]).
 *
 * @param query What was searched for, which the message is scrolled to once the conversation shows.
 * @param messageIndex Where the message is among the conversation's kept messages.
 * @param messageText The message as it was kept, which finds it in the conversation once that shows
 *   ([KeptMessage.isKeptAs]).
 * @param laterDuplicates How many later messages of the conversation were kept with the same text and from the same side,
 *   which tells this one apart from them: counted from the end, since kept messages leave out the oldest of a long
 *   conversation, which the agent's history still has.
 * @param isHeldByAnotherAgent Whether an agent other than the one chosen holds the conversation, which only that agent
 *   can continue, so it is read from its kept messages instead.
 * @param snippet The words around what matched, with what matched marked.
 */
public data class ConversationMatch(
    val conversation: SavedConversation,
    val query: String,
    val messageIndex: Int,
    val messageText: String,
    val laterDuplicates: Int,
    val isFromUser: Boolean,
    val isHeldByAnotherAgent: Boolean,
    val snippet: TextSnippet,
)
