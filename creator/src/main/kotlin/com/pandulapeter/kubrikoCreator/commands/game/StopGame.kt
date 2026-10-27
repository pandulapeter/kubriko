/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.game

import com.pandulapeter.kubrikoCreator.commands.ActionCommand
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.NoInput
import com.pandulapeter.kubrikoCreator.commands.project.toJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal object StopGame : ActionCommand<NoInput> {
    override val name = "stop_game"
    override val description = "Stops the game start_game started, and returns whether one was running."
    override val inputSerializer = NoInput.serializer()

    override suspend fun run(context: CommandContext, input: NoInput): CommandOutcome {
        val result = context.runner.stopGame()
        return CommandOutcome.Success(
            buildJsonObject {
                put("wasRunning", result.isSuccessful)
                put("console", result.console.toJsonArray())
            },
        )
    }
}
