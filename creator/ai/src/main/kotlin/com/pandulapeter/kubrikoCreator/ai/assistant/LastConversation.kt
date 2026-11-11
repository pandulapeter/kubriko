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

/**
 * How a new conversation hears about the project's previous one, so that the user needn't explain again: its title and
 * what it noted for the next one, or else the start of the agent's last answer, after what it is told about the project.
 */
internal object LastConversation {
    const val ANSWER_LENGTH = 1_000

    fun withProject(projectContext: String?, last: SavedConversation?): String? {
        val lastTime = when {
            last == null -> null
            last.summary != null && last.isSummaryEarlier && last.lastAnswer != null ->
                "${notedBy(last)}\nAfter that, it ended with this answer:\n${last.lastAnswer}"
            last.summary != null -> notedBy(last)
            last.lastAnswer != null -> "The previous conversation about this project, \"${last.title}\", ended with this answer:\n${last.lastAnswer}"
            else -> null
        }
        return listOfNotNull(projectContext, lastTime).joinToString("\n\n").ifEmpty { null }
    }

    private fun notedBy(last: SavedConversation) = "What the previous conversation about this project, \"${last.title}\", noted for this one:\n${last.summary}"
}
