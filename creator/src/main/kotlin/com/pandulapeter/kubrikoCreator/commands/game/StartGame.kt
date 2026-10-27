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

internal object StartGame : ActionCommand<NoInput> {
    override val name = "start_game"
    override val description = "Builds the project and starts the game in a window of its own, and leaves it running, so that press_keys, wait, and " +
        "take_screenshot can play it like a player would, to check that a change works. Returns whether it started, with what the console showed " +
        "(compiler problems, the game's logs). Stop it with stop_game. A game that was already running is restarted."
    override val inputSerializer = NoInput.serializer()

    override suspend fun run(context: CommandContext, input: NoInput): CommandOutcome {
        val result = context.runner.startGame()
        return CommandOutcome.Success(
            buildJsonObject {
                put("hasStarted", result.isSuccessful)
                put("console", result.console.toJsonArray())
            },
        )
    }
}
