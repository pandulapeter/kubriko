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

import com.pandulapeter.kubrikoCreator.ai.assistant.TranscriptItem

/**
 * Where among [items] the message a search found is: the message holding [query] (ignoring case) that has
 * [laterOccurrences] more such messages after it, or null while the transcript doesn't hold that many yet (its history
 * still arriving).
 */
internal fun jumpIndexOf(items: List<TranscriptItem>, query: String, laterOccurrences: Int): Int? {
    val holding = items.indices.filter { index ->
        when (val item = items[index]) {
            is TranscriptItem.UserMessage -> item.text.contains(query, ignoreCase = true)
            is TranscriptItem.AgentMessage -> item.text.contains(query, ignoreCase = true)
            else -> false
        }
    }
    return holding.getOrNull(holding.size - 1 - laterOccurrences)
}
