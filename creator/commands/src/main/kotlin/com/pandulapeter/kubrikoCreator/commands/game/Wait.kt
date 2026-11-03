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
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

public object Wait : ActionCommand<Wait.Input> {
    override val name: String = "wait"
    override val description: String = "Lets the game start_game started run for a while without input, and returns what the console showed meanwhile."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.Game

    override suspend fun run(context: CommandContext, input: Input): CommandOutcome {
        if (input.milliseconds !in 1..MAXIMUM_MILLISECONDS) return CommandOutcome.Rejected(Rejection(CommandProblem.GAME_DURATION_INVALID, MAXIMUM_MILLISECONDS.toString()))
        return context.runner.wait(input.milliseconds.toLong()).toOutcome()
    }

    private const val MAXIMUM_MILLISECONDS = 30_000

    @Serializable
    public data class Input(
        @Description("How long to wait, in milliseconds, at most 30000.")
        @SerialName("milliseconds") val milliseconds: Int,
    )
}
