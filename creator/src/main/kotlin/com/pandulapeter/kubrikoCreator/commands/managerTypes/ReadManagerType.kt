/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.managerTypes

import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandOutcome
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireBlockTypes
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireManagerType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateManagerType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_read_manager_type

internal object ReadManagerType : ReadCommand<ReadManagerType.Input> {
    override val name = "read_manager_type"
    override val description = "Reads a block-built Manager type: its Properties, its Events with their Blocks, in order, and the texts it shows. " +
        "Events, Blocks, and texts are addressed by their index in these lists. isChangedByHand tells whether its generated Kotlin class was edited outside Kubriko Creator."
    override val inputSerializer = Input.serializer()
    override val title = Res.string.command_read_manager_type

    private val json = Json { encodeDefaults = true; explicitNulls = false }

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.managerType)

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome {
        context.validateManagerType(input.managerType)?.let { return CommandOutcome.Rejected(it) }
        val definition = json.encodeToJsonElement(ManagerTypeDefinition.serializer(), context.requireManagerType(input.managerType)).jsonObject
        return CommandOutcome.Success(JsonObject(definition + ("isChangedByHand" to JsonPrimitive(input.managerType in context.requireBlockTypes.changedByHand.value))))
    }

    @Serializable
    data class Input(
        @Description("The name of the Manager type.")
        @SerialName("managerType") val managerType: String,
    )
}
