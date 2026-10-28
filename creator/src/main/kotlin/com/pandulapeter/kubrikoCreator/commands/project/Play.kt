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

import com.pandulapeter.kubrikoCreator.commands.ActionCommand
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.Rejection
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_play
import kubriko.creator.generated.resources.error_command_play_duration_invalid

internal object Play : ActionCommand<Play.Input> {
    override val name = "play"
    override val description = "Builds the project and plays the game in a window of its own for a few seconds, then stops it. " +
        "Returns whether it started, with what the console showed meanwhile: compiler problems, the game's logs, and errors."
    override val inputSerializer = Input.serializer()
    override val title = Res.string.command_play

    override fun target(context: CommandContext, input: Input) = CommandTarget.Game

    override suspend fun run(context: CommandContext, input: Input): CommandOutcome {
        if (input.seconds !in 1..MAXIMUM_SECONDS) return CommandOutcome.Rejected(Rejection(Res.string.error_command_play_duration_invalid, MAXIMUM_SECONDS.toString()))
        val result = context.runner.play(durationInMilliseconds = input.seconds * 1000L)
        return CommandOutcome.Success(
            buildJsonObject {
                put("hasStarted", result.isSuccessful)
                put("console", result.console.toJsonArray())
            },
        )
    }

    private const val MAXIMUM_SECONDS = 60

    @Serializable
    data class Input(
        @Description("How many seconds to play the game for, from when it is up, between 1 and 60.")
        @SerialName("seconds") val seconds: Int = 5,
    )
}
