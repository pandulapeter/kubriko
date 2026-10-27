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
import com.pandulapeter.kubrikoCreator.actors.withTextReplaced
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
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_edit_text

@OptIn(EditorMutation::class)
internal object EditText : WriteCommand<EditText.Input> {
    override val name = "edit_text"
    override val description = "Replaces a text a block-built Manager type shows, to change its words, position, size, or color."
    override val title = Res.string.command_edit_text
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.managerType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateTextIndex(input.managerType, input.index) ?: validateText(input.text)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireManagerType(input.managerType)
        context.requireBlockTypes.put(definition.withTextReplaced(input.index, input.text))
        return Applied(inverse = Invocation(EditText, input.copy(text = definition.texts[input.index])))
    }

    override fun mergeKey(input: Input) = Triple(input.managerType, input.index, name)

    @Serializable
    data class Input(
        @Description("The name of the Manager type.")
        @SerialName("managerType") val managerType: String,
        @Description("The index of the text in the Manager type's list of texts, starting from 0.")
        @SerialName("index") val index: Int,
        @Description("The text as it should be.")
        @SerialName("text") val text: ScreenTextDefinition,
    )
}
