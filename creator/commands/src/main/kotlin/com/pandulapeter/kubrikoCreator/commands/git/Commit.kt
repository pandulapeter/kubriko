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
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.git.CommitResult
import com.pandulapeter.kubrikoCreator.git.GitState
import com.pandulapeter.kubrikoCreator.git.GitStatus
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object Commit : WriteCommand<Commit.Input> {
    override val name: String = "commit"
    override val description: String = "Records every change of the project (new, changed, and deleted files alike) in a new git commit."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.VersionControl

    /**
     * Whether there is anything to commit is decided when applying, from the repository's status after the writes: the
     * Version control panel's state may not have caught up with the latest change yet.
     */
    override fun validate(context: CommandContext, input: Input): Rejection? = when (context.git.state.value) {
        GitState.Loading, GitState.NoRepository -> Rejection(CommandProblem.NO_REPOSITORY)
        is GitState.Ready -> if (input.message.isBlank()) Rejection(CommandProblem.COMMIT_MESSAGE_EMPTY) else null
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        if (!context.awaitWrites()) return Applied.failed(context.unsavedWrite())
        return when (context.git.commit(input.message)) {
            CommitResult.Committed -> Applied(inverse = null)
            CommitResult.NothingToCommit -> Applied.failed(CommandProblem.NOTHING_TO_COMMIT)
            CommitResult.Conflicts -> Applied.failed(CommandProblem.COMMIT_CONFLICTS)
            CommitResult.Failed -> Applied.failed(GitStatus.FAILED)
        }
    }

    @Serializable
    public data class Input(
        @Description("The commit message: one sentence saying what changed.")
        @SerialName("message") val message: String,
    )
}
