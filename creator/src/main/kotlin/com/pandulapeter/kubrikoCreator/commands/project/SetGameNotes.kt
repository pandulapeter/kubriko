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

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.project.GameNotes
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_game_notes_too_long

@OptIn(EditorMutation::class)
internal object SetGameNotes : WriteCommand<SetGameNotes.Input> {
    override val name = "set_game_notes"
    override val description = "Replaces the game's design notes (AGENTS.md at the project's root, Markdown) with text, or deletes them for none. Keep them " +
        "short: mechanics, naming, and decisions worth knowing in the next conversation, not what the project digest already says. Propose a change " +
        "in your answer first; the user keeps or undoes it."
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? =
        if ((input.text?.length ?: 0) > MAXIMUM_LENGTH) Rejection(Res.string.error_command_game_notes_too_long, MAXIMUM_LENGTH.toString()) else null

    override suspend fun apply(context: CommandContext, input: Input): Applied = withContext(context.ioDispatcher) {
        val root = context.project.rootDirectory
        val previous = GameNotes.read(root)
        GameNotes.write(root, input.text?.takeIf { it.isNotBlank() })
        Applied(inverse = Invocation(SetGameNotes, Input(previous)))
    }

    private const val MAXIMUM_LENGTH = 20_000

    @Serializable
    data class Input(
        @Description("The notes, in Markdown, or none to delete them.")
        @SerialName("text") val text: String? = null,
    )
}
