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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(EditorMutation::class)
public object DeleteActorType : WriteCommand<DeleteActorType.Input> {
    override val name: String = "delete_actor_type"
    override val description: String = "Deletes a block-built Actor type with its generated Kotlin class."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.BlockType(input.actorType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateActorType(input.actorType)
        ?: ActorTypeFamily(context.requireBlockTypes.actorTypes.value).childrenOf(input.actorType).firstOrNull()?.let { child ->
            Rejection(CommandProblem.ACTOR_TYPE_HAS_CHILDREN, input.actorType, child)
        }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireActorType(input.actorType)
        context.requireBlockTypes.delete(input.actorType)
        return Applied(inverse = Invocation(CreateActorType, definition))
    }

    @Serializable
    public data class Input(
        @Description("The name of the Actor type to delete.")
        @SerialName("actorType") val actorType: String,
    )
}
