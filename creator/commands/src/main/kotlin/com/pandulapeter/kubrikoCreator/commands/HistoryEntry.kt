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
 * One step of the undo or redo history: the change that [revert] reverts and [reapply] makes again.
 *
 * @param conversationId The tool address of the assistant's conversation that made the change, if one did.
 * @param revision The project's revision once the change was made ([ChangeLog]).
 * @param isByAi Whether an AI client made the change.
 * @param contentHashes A hash of each type and Scene [revert] and [reapply] touch, as it was right after the change was made
 *   or stepped ([CommandContext.contentHashesOf]), so that a step whose type or Scene changed outside Kubriko Creator since
 *   is dropped instead of applied.
 */
public data class HistoryEntry(
    val revert: Invocation<*>,
    val reapply: Invocation<*>,
    val conversationId: String? = null,
    val revision: Int = 0,
    val isByAi: Boolean = false,
    val contentHashes: Map<CommandTarget, Int> = emptyMap(),
) {
    /**
     * The change the user made, which names the step in both directions.
     */
    internal val commandName get() = reapply.commandName
}
