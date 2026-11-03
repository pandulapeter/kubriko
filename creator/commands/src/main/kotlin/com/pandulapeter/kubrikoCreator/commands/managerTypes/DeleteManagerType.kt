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

import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireBlockTypes
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireManagerType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateManagerType
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object DeleteManagerType : WriteCommand<DeleteManagerType.Input> {
    override val name: String = "delete_manager_type"
    override val description: String = "Deletes a block-built Manager type with its generated Kotlin class. Blocks of other types that use its Properties stay, but do nothing until it is created again."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.BlockType(input.managerType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateManagerType(input.managerType)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireManagerType(input.managerType)
        context.requireBlockTypes.delete(input.managerType)
        return Applied(inverse = Invocation(CreateManagerType, definition))
    }

    @Serializable
    public data class Input(
        @Description("The name of the Manager type to delete.")
        @SerialName("managerType") val managerType: String,
    )
}
