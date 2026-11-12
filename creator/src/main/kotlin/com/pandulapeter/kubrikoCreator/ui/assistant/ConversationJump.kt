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

import com.pandulapeter.kubrikoCreator.ai.assistant.ConversationMatch
import com.pandulapeter.kubrikoCreator.ai.assistant.KeptMessage
import com.pandulapeter.kubrikoCreator.ai.assistant.TranscriptItem

/**
 * Where among [items] the message a search found ([match]) is: the message kept with its text, with as many later ones
 * kept alike after it as it had, or null when the conversation doesn't hold it.
 */
internal fun jumpIndexOf(items: List<TranscriptItem>, match: ConversationMatch): Int? {
    val message = KeptMessage(match.isFromUser, match.messageText)
    val alike = items.indices.filter { KeptMessage.isKeptAs(items[it], message) }
    return alike.getOrNull(alike.size - 1 - match.laterDuplicates)
}
