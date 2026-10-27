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
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.Rejection
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_game_duration_invalid
import kubriko.creator.generated.resources.command_wait

internal object Wait : ActionCommand<Wait.Input> {
    override val name = "wait"
    override val description = "Lets the game start_game started run for a while without input, and returns what the console showed meanwhile."
    override val inputSerializer = Input.serializer()
    override val title = Res.string.command_wait

    override fun target(context: CommandContext, input: Input) = CommandTarget.Game

    override suspend fun run(context: CommandContext, input: Input): CommandOutcome {
        if (input.milliseconds !in 1..MAXIMUM_MILLISECONDS) return CommandOutcome.Rejected(Rejection(Res.string.error_game_duration_invalid, MAXIMUM_MILLISECONDS.toString()))
        return context.runner.wait(input.milliseconds.toLong()).toOutcome()
    }

    private const val MAXIMUM_MILLISECONDS = 30_000

    @Serializable
    data class Input(
        @Description("How long to wait, in milliseconds, at most 30000.")
        @SerialName("milliseconds") val milliseconds: Int,
    )
}
