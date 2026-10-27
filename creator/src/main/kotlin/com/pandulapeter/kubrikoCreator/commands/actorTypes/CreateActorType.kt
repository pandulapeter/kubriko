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
import com.pandulapeter.kubrikoCreator.actors.ActorTypeNaming
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_create_actor_type
import kubriko.creator.generated.resources.error_command_no_actor_types

@OptIn(EditorMutation::class)
internal object CreateActorType : WriteCommand<ActorTypeDefinition> {
    override val name = "create_actor_type"
    override val description = "Creates a block-built Actor type. Only the name is required: a new Actor type is a circle without Events. " +
        "Kubriko Creator generates its Kotlin class, and it can be placed in Scenes once the project is rebuilt."
    override val title = Res.string.command_create_actor_type
    override val inputSerializer = ActorTypeDefinition.serializer()

    override fun target(context: CommandContext, input: ActorTypeDefinition) = CommandTarget.ActorType(input.name)

    override fun validate(context: CommandContext, input: ActorTypeDefinition): Rejection? {
        val actorTypes = context.actorTypes ?: return Rejection(Res.string.error_command_no_actor_types)
        val names = actorTypes.actorTypes.value.map { it.name }
        return ActorTypeNaming.validate(input.name, names)?.let(::Rejection) ?: input.validateContents(names)
    }

    override suspend fun apply(context: CommandContext, input: ActorTypeDefinition): Applied {
        context.requireActorTypes.put(input)
        return Applied(inverse = Invocation(DeleteActorType, DeleteActorType.Input(input.name)))
    }
}
