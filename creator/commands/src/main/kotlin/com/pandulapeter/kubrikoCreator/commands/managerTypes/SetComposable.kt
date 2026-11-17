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

import com.pandulapeter.kubrikoCreator.actors.codegen.ManagerComposables
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Descriptions
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
public object SetComposable : WriteCommand<SetComposable.Input> {
    override val name: String = "set_composable"
    override val description: String = "Sets the project's @Composable a Manager type shows after its texts and Kotlin UI, as create_composable makes one; an empty name for none."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.BlockType(input.managerType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateManagerType(input.managerType)
        ?: input.composable.takeIf { it.isNotEmpty() && !ManagerComposables.isValid(it) }?.let { Rejection(CommandProblem.COMPOSABLE_NAME_INVALID, it) }

    override fun validateRestoring(context: CommandContext, input: Input): Rejection? = context.validateManagerType(input.managerType)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireManagerType(input.managerType)
        context.requireBlockTypes.put(definition.copy(composable = input.composable.ifEmpty { null }))
        return Applied(inverse = Invocation(SetComposable, input.copy(composable = definition.composable.orEmpty())))
    }

    @Serializable
    public data class Input(
        @Description(Descriptions.MANAGER_TYPE_NAME)
        @SerialName("managerType") val managerType: String,
        @Description("The function's fully qualified name, like com.example.game.ui.HudUi, or an empty text for none.")
        @SerialName("composable") val composable: String,
    )
}
