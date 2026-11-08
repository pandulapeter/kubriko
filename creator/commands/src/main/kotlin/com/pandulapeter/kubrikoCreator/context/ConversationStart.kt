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

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.project.GameNotes
import kotlinx.coroutines.withContext

/**
 * What an assistant conversation is told about the project at its start: the digest, then the screenplay's page index
 * ([PageIndexText]), then the game's notes, cut short past [NOTES_LIMIT] characters.
 */
public object ConversationStart {
    private const val NOTES_LIMIT = 6_000

    public suspend fun of(context: CommandContext): String {
        val notes = withContext(context.ioDispatcher) { GameNotes.read(context.project.rootDirectory) }?.trim()?.takeIf { it.isNotEmpty() }
        val pages = withContext(context.ioDispatcher) { context.screenplay.scanNames().mapNotNull { name -> context.screenplay.read(name)?.let { name to it } }.toMap() }
        return DigestText.of(context.projectDigest()) + PageIndexText.of(pages)?.let { "\n\n$it" }.orEmpty() + notes?.let { "\n\nThe game's notes (${GameNotes.FILE_NAME}):\n" + it.take(NOTES_LIMIT) + if (it.length > NOTES_LIMIT) "\n…(read_game_notes reads the rest)" else "" }.orEmpty()
    }
}
