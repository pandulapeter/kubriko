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
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.ReadCommand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireManagerType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.typeReadResult
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateManagerType
import com.pandulapeter.kubrikoCreator.context.ReadDetail
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

public object ReadManagerType : ReadCommand<ReadManagerType.Input> {
    override val name: String = "read_manager_type"
    override val description: String = "Reads a block-built Manager type: its Properties, its Events with their Blocks, in order, the texts it shows, and its Kotlin UI, " +
        "written as the editor shows them (detail json returns the JSON the write tools take). Events, Blocks, and texts are addressed by their index, written before each. " +
        "isChangedByHand, there when true, says that its generated Kotlin class was edited outside Kubriko Creator."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.BlockType(input.managerType, input.eventIndex)

    override suspend fun read(context: CommandContext, input: Input): CommandOutcome {
        context.validateManagerType(input.managerType)?.let { return CommandOutcome.Rejected(it) }
        return context.typeReadResult(context.requireManagerType(input.managerType), ManagerTypeDefinition.serializer(), input.detail, input.eventIndex)
    }

    @Serializable
    public data class Input(
        @Description(Descriptions.MANAGER_TYPE_NAME)
        @SerialName("managerType") val managerType: String,
        @Description("How much of it to return: outline (Events by title), blocks (Events with their Blocks, the default), or json (everything, as the write tools take it).")
        @SerialName("detail") val detail: ReadDetail = ReadDetail.BLOCKS,
        @Description("Only the Event at this index, in full, and nothing else of the type.")
        @SerialName("eventIndex") val eventIndex: Int? = null,
    )
}
