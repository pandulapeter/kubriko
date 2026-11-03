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
import com.pandulapeter.kubrikoCreator.git.GitState
import com.pandulapeter.kubrikoCreator.git.GitStatus
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
internal object DiscardChanges : WriteCommand<DiscardChanges.Input> {
    override val name = "discard_changes"
    override val description = "Restores files to how the last commit has them, deleting the ones it doesn't have. This can't be undone."
    override val inputSerializer = Input.serializer()
    override val isDestructive = true

    override fun target(context: CommandContext, input: Input) = CommandTarget.VersionControl

    override fun validate(context: CommandContext, input: Input): Rejection? = when (val state = context.git.state.value) {
        GitState.Loading, GitState.NoRepository -> Rejection(CommandProblem.NO_REPOSITORY)
        is GitState.Ready -> {
            val changedPaths = state.changes.flatMap { it.paths }.toSet()
            input.paths.firstOrNull { it !in changedPaths }?.let { Rejection(CommandProblem.CHANGE_MISSING, it) }
        }
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        if (!context.awaitWrites()) return Applied.failed(context.unsavedWrite())
        if (!context.git.discard(input.paths)) return Applied.failed(GitStatus.FAILED)
        context.reloadTypes()
        return Applied(inverse = null, isRewritingFiles = true)
    }

    @Serializable
    data class Input(
        @Description("The changed files to restore, relative to the project folder, as the version control status lists them.")
        @SerialName("paths") val paths: List<String>,
    )
}
