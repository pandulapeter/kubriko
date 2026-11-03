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
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.project.toJsonArray
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

public object StartGame : ActionCommand<StartGame.Input> {
    override val name: String = "start_game"
    override val description: String = "Builds the project and starts the game in a window of its own, and leaves it running, so that press_keys, wait, and " +
        "take_screenshot can play it like a player would, to check that a change works. Returns whether it started, with what the console showed " +
        "(compiler problems, the game's logs). Stop it with stop_game. A game that was already running is restarted. Give a scene to start " +
        "there instead of in the game's first Scene."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    @Serializable
    public data class Input(
        @Description("The Scene to start the game in, by its name (levels/1-1), or none for the game's first Scene.")
        val scene: String? = null,
        @Description("Where to start it, from list_play_targets, or none for the desktop.")
        val target: String? = null,
    )

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Game

    override suspend fun run(context: CommandContext, input: Input): CommandOutcome {
        if (input.scene != null && !context.scenes.exists(input.scene)) {
            return CommandOutcome.Rejected(Rejection(CommandProblem.SCENE_MISSING, input.scene))
        }
        val target = when (val lookup = PlayTargetLookup.of(context, input.target)) {
            is PlayTargetLookup.Found -> lookup.target
            is PlayTargetLookup.Missing -> return lookup.outcome
        }
        val result = context.runner.startGame(firstScene = input.scene, target = target)
        return CommandOutcome.Success(
            buildJsonObject {
                put("hasStarted", result.isSuccessful)
                put("console", result.console.toJsonArray())
            },
        )
    }
}
