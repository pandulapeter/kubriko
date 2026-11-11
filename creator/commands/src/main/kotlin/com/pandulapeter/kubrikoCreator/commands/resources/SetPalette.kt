/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.resources

import com.pandulapeter.kubrikoCreator.actors.HexColorSerializer
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException

@OptIn(EditorMutation::class)
public object SetPalette : WriteCommand<SetPalette.Input> {
    override val name: String = "set_palette"
    override val description: String = "Sets the colors the project keeps for drawing its sprites, which read_sprite and the sprite editor offer: " +
        "the same few colors across sprites make the game look of one piece."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = when {
        input.colors.size > MAXIMUM_COLORS -> Rejection(SpriteProblem.PALETTE_TOO_LARGE, MAXIMUM_COLORS.toString())
        else -> null
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val previous = context.palette.colors.value
        return try {
            withContext(context.ioDispatcher) { context.palette.set(input.colors.distinct()) }
            Applied(inverse = Invocation(SetPalette, Input(previous)))
        } catch (exception: IOException) {
            Applied.failed(Rejection(SpriteProblem.PALETTE_NOT_WRITTEN, exception.message ?: exception.toString()))
        }
    }

    private const val MAXIMUM_COLORS = 64

    @Serializable
    public data class Input(
        @Description("Every color, as #RRGGBB, or #AARRGGBB when it isn't opaque; an empty list for none.")
        @SerialName("colors") val colors: List<@Serializable(with = HexColorSerializer::class) Long>,
    )
}
