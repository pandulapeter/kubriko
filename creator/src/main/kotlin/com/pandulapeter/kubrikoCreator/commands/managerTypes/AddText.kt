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

import com.pandulapeter.kubrikoCreator.actors.ScreenTextDefinition
import com.pandulapeter.kubrikoCreator.actors.withTextInserted
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireBlockTypes
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireManagerType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateManagerType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_add_text

@OptIn(EditorMutation::class)
internal object AddText : WriteCommand<AddText.Input> {
    override val name = "add_text"
    override val description = "Adds a text that a block-built Manager type shows on top of the game, like \"Score: {score}\", which follows the Properties it shows."
    override val title = Res.string.command_add_text
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.managerType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateManagerType(input.managerType) ?: validateText(input.text)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireManagerType(input.managerType)
        val index = input.index?.coerceIn(0, definition.texts.size) ?: definition.texts.size
        context.requireBlockTypes.put(definition.withTextInserted(index, input.text))
        return Applied(inverse = Invocation(RemoveText, RemoveText.Input(input.managerType, index)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Manager type.")
        @SerialName("managerType") val managerType: String,
        @Description("The text to add.")
        @SerialName("text") val text: ScreenTextDefinition,
        @Description("Where the text goes in the Manager type's list of texts, starting from 0. Last if left out.")
        @SerialName("index") val index: Int? = null,
    )
}
