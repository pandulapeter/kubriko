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

import com.pandulapeter.kubrikoCreator.actors.withTextRemoved
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireBlockTypes
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireManagerType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_remove_text

@OptIn(EditorMutation::class)
internal object RemoveText : WriteCommand<RemoveText.Input> {
    override val name = "remove_text"
    override val description = "Removes a text a block-built Manager type shows."
    override val title = Res.string.command_remove_text
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.managerType)

    override fun validate(context: CommandContext, input: Input) = context.validateTextIndex(input.managerType, input.index)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireManagerType(input.managerType)
        context.requireBlockTypes.put(definition.withTextRemoved(input.index))
        return Applied(inverse = Invocation(AddText, AddText.Input(input.managerType, definition.texts[input.index], input.index)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Manager type.")
        @SerialName("managerType") val managerType: String,
        @Description("The index of the text in the Manager type's list of texts, starting from 0.")
        @SerialName("index") val index: Int,
    )
}
