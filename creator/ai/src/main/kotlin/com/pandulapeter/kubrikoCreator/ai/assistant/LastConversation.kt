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
 * the start of the agent's last answer, after what it is told about the project.
 */
internal object LastConversation {
    const val ANSWER_LENGTH = 1_000

    fun withProject(projectContext: String?, last: SavedConversation?): String? {
        val lastTime = last?.lastAnswer?.let { answer -> "The previous conversation about this project, \"${last.title}\", ended with this answer:\n$answer" }
        return listOfNotNull(projectContext, lastTime).joinToString("\n\n").ifEmpty { null }
    }
}
