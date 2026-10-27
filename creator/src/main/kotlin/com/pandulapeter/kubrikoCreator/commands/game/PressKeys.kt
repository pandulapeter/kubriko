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
import kubriko.creator.generated.resources.error_game_key_unknown
import kubriko.creator.generated.resources.error_game_keys_count
import kubriko.creator.generated.resources.command_press_keys

internal object PressKeys : ActionCommand<PressKeys.Input> {
    override val name = "press_keys"
    override val description = "Holds keys down together in the game start_game started, for a while, then releases them, while the game keeps running. " +
        "Returns what the console showed meanwhile. Combine it with take_screenshot to see what the keys did."
    override val inputSerializer = Input.serializer()
    override val title = Res.string.command_press_keys

    override fun target(context: CommandContext, input: Input) = CommandTarget.Game

    override suspend fun run(context: CommandContext, input: Input): CommandOutcome {
        when {
            input.keys.size !in 1..MAXIMUM_KEYS -> return CommandOutcome.Rejected(Rejection(Res.string.error_game_keys_count, MAXIMUM_KEYS.toString()))
            input.milliseconds !in 1..MAXIMUM_MILLISECONDS -> return CommandOutcome.Rejected(Rejection(Res.string.error_game_duration_invalid, MAXIMUM_MILLISECONDS.toString()))
        }
        input.keys.firstOrNull { !isKeyName(it) }?.let { return CommandOutcome.Rejected(Rejection(Res.string.error_game_key_unknown, it)) }
        return context.runner.pressKeys(input.keys.map { it.uppercase() }, input.milliseconds.toLong()).toOutcome()
    }

    private const val MAXIMUM_KEYS = 10
    private const val MAXIMUM_MILLISECONDS = 10_000

    @Serializable
    data class Input(
        @Description("The keys, named as in Java's KeyEvent without VK_: LEFT, RIGHT, UP, DOWN, SPACE, ENTER, ESCAPE, SHIFT, CONTROL, A to Z, 0 to 9.")
        @SerialName("keys") val keys: List<String>,
        @Description("How long to hold them, in milliseconds, at most 10000. A short tap is about 100.")
        @SerialName("milliseconds") val milliseconds: Int = 150,
    )
}
