/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.actorTypes

import com.pandulapeter.kubrikoCreator.actors.ActorShape
import com.pandulapeter.kubrikoCreator.actors.HexColorSerializer
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_set_appearance

@OptIn(EditorMutation::class)
internal object SetAppearance : WriteCommand<SetAppearance.Input> {
    override val name = "set_appearance"
    override val description = "Changes how the Actors of a block-built Actor type look. Properties left out stay as they are."
    override val title = Res.string.command_set_appearance
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.ActorType(input.actorType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateActorType(input.actorType) ?: run {
        val definition = context.requireActorType(input.actorType)
        validateSize(input.width ?: definition.width, input.height ?: definition.height)
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireActorType(input.actorType)
        context.requireActorTypes.put(
            definition.copy(
                shape = input.shape ?: definition.shape,
                color = input.color ?: definition.color,
                width = input.width ?: definition.width,
                height = input.height ?: definition.height,
            ),
        )
        return Applied(
            inverse = Invocation(
                command = SetAppearance,
                input = Input(
                    actorType = input.actorType,
                    shape = input.shape?.let { definition.shape },
                    color = input.color?.let { definition.color },
                    width = input.width?.let { definition.width },
                    height = input.height?.let { definition.height },
                ),
            ),
        )
    }

    override fun mergeKey(input: Input) = listOf(input.actorType, input.shape != null, input.color != null, input.width != null, input.height != null)

    @Serializable
    data class Input(
        @Description("The name of the Actor type.")
        @SerialName("actorType") val actorType: String,
        @Description("The shape its Actors are drawn as.")
        @SerialName("shape") val shape: ActorShape? = null,
        @Description("The color its Actors are drawn with, as #RRGGBB, or #AARRGGBB when it isn't opaque.")
        @SerialName("color") @Serializable(with = HexColorSerializer::class) val color: Long? = null,
        @Description("The width of a newly placed Actor, in scene units.")
        @SerialName("width") val width: Float? = null,
        @Description("The height of a newly placed Actor, in scene units.")
        @SerialName("height") val height: Float? = null,
    )
}
