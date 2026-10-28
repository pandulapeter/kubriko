/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.git

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.git.GitState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_commit
import kubriko.creator.generated.resources.error_command_commit_message_empty
import kubriko.creator.generated.resources.error_command_no_repository
import kubriko.creator.generated.resources.error_command_nothing_to_commit
import kubriko.creator.generated.resources.status_git_failed

@OptIn(EditorMutation::class)
internal object Commit : WriteCommand<Commit.Input> {
    override val name = "commit"
    override val description = "Records every change of the project (new, changed, and deleted files alike) in a new git commit."
    override val title = Res.string.command_commit
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.VersionControl

    override fun validate(context: CommandContext, input: Input): Rejection? = when (val state = context.git.state.value) {
        GitState.Loading, GitState.NoRepository -> Rejection(Res.string.error_command_no_repository)
        is GitState.Ready -> when {
            input.message.isBlank() -> Rejection(Res.string.error_command_commit_message_empty)
            state.changes.isEmpty() -> Rejection(Res.string.error_command_nothing_to_commit)
            else -> null
        }
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        context.awaitWrites()
        return if (context.git.commit(input.message)) Applied(inverse = null) else Applied.failed(Res.string.status_git_failed)
    }

    @Serializable
    data class Input(
        @Description("The commit message: one sentence saying what changed.")
        @SerialName("message") val message: String,
    )
}
