/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.project

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.project.GameNotes
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonPrimitive

public object ReadGameNotes : ReadCommand<NoInput> {
    override val name: String = "read_game_notes"
    override val description: String = "Reads the game's design notes (AGENTS.md at the project's root): its mechanics, naming, and decisions, kept across " +
        "conversations. Every conversation starts with them, so read them again only after they changed."
    override val inputSerializer: KSerializer<NoInput> = NoInput.serializer()

    override suspend fun read(context: CommandContext, input: NoInput): CommandOutcome = CommandOutcome.Success(
        JsonPrimitive(withContext(context.ioDispatcher) { GameNotes.read(context.project.rootDirectory) } ?: "The game has no notes yet."),
    )
}
