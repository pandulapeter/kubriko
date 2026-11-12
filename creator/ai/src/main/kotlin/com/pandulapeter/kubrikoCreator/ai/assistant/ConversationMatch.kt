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
 * @param occurrence How many earlier messages of the conversation hold [query] too, which tells this one apart from them
 *   once the conversation shows it.
 * @param isHeldByAnotherAgent Whether an agent other than the one chosen holds the conversation, which only that agent
 *   can continue, so it is read from its kept messages instead.
 * @param snippet The words around what matched, with what matched marked.
 */
public data class ConversationMatch(
    val conversation: SavedConversation,
    val query: String,
    val occurrence: Int,
    val isFromUser: Boolean,
    val isHeldByAnotherAgent: Boolean,
    val snippet: TextSnippet,
)
