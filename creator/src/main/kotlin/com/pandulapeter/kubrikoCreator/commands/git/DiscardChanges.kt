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
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.git.GitState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_discard_changes
import kubriko.creator.generated.resources.error_command_change_missing
import kubriko.creator.generated.resources.status_git_failed
import kubriko.creator.generated.resources.error_command_no_repository

@OptIn(EditorMutation::class)
internal object DiscardChanges : WriteCommand<DiscardChanges.Input> {
    override val name = "discard_changes"
    override val description = "Restores files to how the last commit has them, deleting the ones it doesn't have. This can't be undone."
    override val title = Res.string.command_discard_changes
    override val inputSerializer = Input.serializer()
    override val isDestructive = true

    override fun validate(context: CommandContext, input: Input): Rejection? = when (val state = context.git.state.value) {
        GitState.Loading, GitState.NoRepository -> Rejection(Res.string.error_command_no_repository)
        is GitState.Ready -> {
            val changedPaths = state.changes.flatMap { it.paths }.toSet()
            input.paths.firstOrNull { it !in changedPaths }?.let { Rejection(Res.string.error_command_change_missing, it) }
        }
    }

    override suspend fun apply(context: CommandContext, input: Input) = if (context.git.discard(input.paths)) Applied(inverse = null) else Applied.failed(Res.string.status_git_failed)

    @Serializable
    data class Input(
        @Description("The changed files to restore, relative to the project folder, as the version control status lists them.")
        @SerialName("paths") val paths: List<String>,
    )
}
