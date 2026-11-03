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
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.git.GitState
import com.pandulapeter.kubrikoCreator.git.GitStatus
import kotlinx.serialization.KSerializer

@OptIn(EditorMutation::class)
public object StartVersionControl : WriteCommand<NoInput> {
    override val name: String = "start_version_control"
    override val description: String = "Puts the project under version control: makes it a git repository on a main branch, with everything in it in a first commit."
    override val inputSerializer: KSerializer<NoInput> = NoInput.serializer()

    override fun target(context: CommandContext, input: NoInput): CommandTarget? = CommandTarget.VersionControl

    override fun validate(context: CommandContext, input: NoInput): Rejection? = when (context.git.state.value) {
        GitState.NoRepository -> null
        GitState.Loading, is GitState.Ready -> Rejection(CommandProblem.ALREADY_UNDER_VERSION_CONTROL)
    }

    override suspend fun apply(context: CommandContext, input: NoInput): Applied {
        if (!context.awaitWrites()) return Applied.failed(context.unsavedWrite())
        return if (context.git.startVersionControl()) Applied(inverse = null) else Applied.failed(GitStatus.FAILED)
    }
}
