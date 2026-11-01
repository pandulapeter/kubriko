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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.context.ReadDetail
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_read_actor_type

internal object ReadActorType : ReadCommand<ReadActorType.Input> {
    override val name = "read_actor_type"
    override val description = "Reads a block-built Actor type: its parent, appearance, Properties, and Events with their Blocks, in order, written as the editor shows them " +
        "(detail json returns the JSON the write tools take). Events and Blocks are addressed by their index, written before each. " +
        "Zoom in on a large type: its outline first, then one Event (eventIndex). isChangedByHand, there when true, says that its generated Kotlin class was edited outside Kubriko Creator."
    override val inputSerializer = Input.serializer()
    override val title = Res.string.command_read_actor_type

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.actorType, input.eventIndex)

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome {
        context.validateActorType(input.actorType)?.let { return CommandOutcome.Rejected(it) }
        return context.typeReadResult(context.requireActorType(input.actorType), ActorTypeDefinition.serializer(), input.detail, input.eventIndex)
    }

    @Serializable
    data class Input(
        @Description("The name of the Actor type.")
        @SerialName("actorType") val actorType: String,
        @Description("How much of it to return: outline (Events by title), blocks (Events with their Blocks, the default), or json (everything, as the write tools take it).")
        @SerialName("detail") val detail: ReadDetail = ReadDetail.BLOCKS,
        @Description("Only the Event at this index, in full, and nothing else of the type.")
        @SerialName("eventIndex") val eventIndex: Int? = null,
    )
}
