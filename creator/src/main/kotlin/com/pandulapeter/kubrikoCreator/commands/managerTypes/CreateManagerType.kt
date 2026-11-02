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

import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.actorTypes.requireBlockTypes
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateContents
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateNewTypeName
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_create_manager_type

@OptIn(EditorMutation::class)
internal object CreateManagerType : WriteCommand<ManagerTypeDefinition> {
    override val name = "create_manager_type"
    override val description = "Creates a block-built Manager type: game-wide values (Properties, like the score or the lives left) and rules (Events " +
        "with Blocks), plus texts shown on top of the game. The game has one of each Manager type. Only the name is required. Blocks of every " +
        "Actor type and Manager type can then use its Properties (through their manager property), and texts and log Blocks can show them " +
        "as {ManagerType.property}. Kubriko Creator generates its Kotlin class and adds it to the generated KubrikoWiring."
    override val title = Res.string.command_create_manager_type
    override val inputSerializer = ManagerTypeDefinition.serializer()

    override fun target(context: CommandContext, input: ManagerTypeDefinition) = CommandTarget.BlockType(input.name)

    override fun validate(context: CommandContext, input: ManagerTypeDefinition): Rejection? = validateRestoring(context, input)
        ?: context.requireBlockTypes.let { types -> input.validateContents(types.actorTypes.value.map { it.name }, types.managerTypes.value) }

    override fun validateRestoring(context: CommandContext, input: ManagerTypeDefinition) = context.validateNewTypeName(input.name, BlockTypeKind.MANAGER)

    override suspend fun apply(context: CommandContext, input: ManagerTypeDefinition): Applied {
        context.requireBlockTypes.put(input)
        return Applied(inverse = Invocation(DeleteManagerType, DeleteManagerType.Input(input.name)))
    }
}
