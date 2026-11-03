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

import com.pandulapeter.kubrikoCreator.commands.ChangeLog
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.context.ConversationStart

/**
 * What a conversation needs from the project open at the time.
 */
public interface OpenProjectAccess {

    /** The digest of the open project, which a new conversation starts with, or null when none is open. */
    public suspend fun digest(): String?

    /** The open project's changes, which a prompt sums up for the agent when the user made some since its last turn. */
    public fun changeLog(): ChangeLog?

    /** Reverts what a conversation changed after a revision ([AssistantChat]'s Undo); how many steps it reverted. */
    public suspend fun undoTurn(toolsId: String, sinceRevision: Int): Int

    public companion object {
        /** No project, ever. */
        public val None: OpenProjectAccess = object : OpenProjectAccess {
            override suspend fun digest(): String? = null

            override fun changeLog(): ChangeLog? = null

            override suspend fun undoTurn(toolsId: String, sinceRevision: Int): Int = 0
        }

        /** The project whose commands [executor] gives, if one is open. */
        public fun of(executor: () -> CommandExecutor?): OpenProjectAccess = object : OpenProjectAccess {
            override suspend fun digest(): String? = executor()?.context?.let { ConversationStart.of(it) }

            override fun changeLog(): ChangeLog? = executor()?.changeLog

            override suspend fun undoTurn(toolsId: String, sinceRevision: Int): Int = executor()?.undoTurn(toolsId, sinceRevision) ?: 0
        }
    }
}
