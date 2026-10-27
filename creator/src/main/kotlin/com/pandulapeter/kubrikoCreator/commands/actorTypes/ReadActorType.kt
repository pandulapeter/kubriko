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
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_read_actor_type

internal object ReadActorType : ReadCommand<ReadActorType.Input> {
    override val name = "read_actor_type"
    override val description = "Reads a block-built Actor type: its appearance and its Events with their Blocks, in order. " +
        "Events and Blocks are addressed by their index in these lists. isChangedByHand tells whether its generated Kotlin class was edited outside Kubriko Creator."
    override val inputSerializer = Input.serializer()
    override val title = Res.string.command_read_actor_type

    private val json = Json { encodeDefaults = true; explicitNulls = false }

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.actorType)

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome {
        context.validateActorType(input.actorType)?.let { return CommandOutcome.Rejected(it) }
        val definition = json.encodeToJsonElement(ActorTypeDefinition.serializer(), context.requireActorType(input.actorType)).jsonObject
        return CommandOutcome.Success(JsonObject(definition + ("isChangedByHand" to JsonPrimitive(input.actorType in context.requireBlockTypes.changedByHand.value))))
    }

    @Serializable
    data class Input(
        @Description("The name of the Actor type.")
        @SerialName("actorType") val actorType: String,
    )
}
