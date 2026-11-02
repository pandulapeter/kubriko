/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.commands.ChangeRecord

/**
 * What an agent is told, before the user's next message, about the changes made since its last turn by anyone but itself
 * (the user, mostly), so that it stays in step without reading everything again: `add_block at Ball#2.1`.
 */
internal object ChangesSummary {

    /**
     * @param records The changes made after revision [since], oldest first, as far as they are kept.
     * @param conversationId The conversation the summary is for, whose own changes are left out.
     * @return The summary, or null if nobody else changed anything.
     */
    fun of(records: List<ChangeRecord>, since: Int, conversationId: String?): String? {
        val others = records.filter { conversationId == null || it.origin?.conversationId != conversationId }
        if (others.isEmpty()) return null
        val forgotten = (records.first().revision - since - 1).coerceAtLeast(0)
        val shown = others.takeLast(MAXIMUM_SHOWN)
        val lines = shown.map { record ->
            buildString {
                if (record.isByAi) append("(another AI client) ")
                when (record.step) {
                    ChangeRecord.Step.UNDO -> append("undid ")
                    ChangeRecord.Step.REDO -> append("redid ")
                    null -> Unit
                }
                append(record.commandName)
                record.target?.let { append(" at ").append(it.notation()) }
            }
        }
        val earlier = forgotten + others.size - shown.size
        return buildString {
            append("Changed in the editor since your last turn, by the user unless marked, oldest first: ")
            if (earlier > 0) append("$earlier earlier changes; ")
            append(lines.joinToString("; "))
            append(". Read again what you need; the read tools say what changed since you last read it.")
        }
    }

    private const val MAXIMUM_SHOWN = 20
}
