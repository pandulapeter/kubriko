/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

/**
 * What the assistant's conversations noted for the next conversation about the project
 * ([com.pandulapeter.kubrikoCreator.commands.project.RememberForNextTime]), by the ID of each one's tool address, until the
 * assistant takes it at the end of the turn.
 */
public class ConversationSummaries {
    private val summaries = HashMap<String, String>()

    /**
     * Keeps [summary] as the conversation [conversationId]'s, in place of an earlier one it hasn't taken yet.
     */
    @Synchronized
    public fun remember(conversationId: String, summary: String) {
        summaries[conversationId] = summary
    }

    /**
     * The conversation [conversationId]'s latest summary since it last took one, which it no longer holds afterwards.
     */
    @Synchronized
    public fun take(conversationId: String): String? = summaries.remove(conversationId)
}
