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
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import kotlinx.serialization.KSerializer

@OptIn(EditorMutation::class)
public object CreateActorType : WriteCommand<ActorTypeDefinition> {
    override val name: String = "create_actor_type"
    override val description: String = "Creates a block-built Actor type. Only the name is required: a new Actor type is a circle without Events. " +
        "Kubriko Creator generates its Kotlin class, and it can be placed in Scenes once the project is rebuilt."
    override val inputSerializer: KSerializer<ActorTypeDefinition> = ActorTypeDefinition.serializer()

    override fun target(context: CommandContext, input: ActorTypeDefinition): CommandTarget? = CommandTarget.BlockType(input.name)

    override fun validate(context: CommandContext, input: ActorTypeDefinition): Rejection? = validateRestoring(context, input)
        ?: context.validateContents(input)

    override fun validateRestoring(context: CommandContext, input: ActorTypeDefinition): Rejection? = context.validateNewTypeName(input.name, BlockTypeKind.ACTOR)

    override suspend fun apply(context: CommandContext, input: ActorTypeDefinition): Applied {
        context.requireBlockTypes.put(input)
        return Applied(inverse = Invocation(DeleteActorType, DeleteActorType.Input(input.name)))
    }
}
