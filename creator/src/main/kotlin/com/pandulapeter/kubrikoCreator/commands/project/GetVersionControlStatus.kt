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
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.git.GitState
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_get_version_control_status

internal object GetVersionControlStatus : ReadCommand<NoInput> {
    override val name = "get_version_control_status"
    override val description = "Tells whether the project is under version control, and if it is, its branch, what changed since the last commit " +
        "(an Actor type's definition and generated code count as one change), and the recent commits."
    override val inputSerializer = NoInput.serializer()
    override val title = Res.string.command_get_version_control_status

    override fun target(context: CommandContext, input: NoInput) = CommandTarget.VersionControl

    override suspend fun read(context: CommandContext, input: NoInput) = CommandOutcome.Success(
        buildJsonObject {
            when (val state = context.git.state.value) {
                GitState.Loading, GitState.NoRepository -> put("isUnderVersionControl", false)
                is GitState.Ready -> {
                    put("isUnderVersionControl", true)
                    state.branch?.let { put("branch", it) }
                    putJsonArray("changes") {
                        state.changes.forEach { change ->
                            addJsonObject {
                                put("title", change.title)
                                put("kind", change.kind.name.lowercase())
                                put("isBlockType", change.isBlockType)
                                putJsonArray("paths") { change.paths.forEach { add(it) } }
                            }
                        }
                    }
                    putJsonArray("history") {
                        state.history.forEach { commit ->
                            addJsonObject {
                                put("id", commit.id)
                                put("message", commit.message)
                                put("author", commit.author)
                            }
                        }
                    }
                }
            }
        },
    )
}
