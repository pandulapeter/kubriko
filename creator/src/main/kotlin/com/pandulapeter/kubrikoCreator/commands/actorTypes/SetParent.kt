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
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Description
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_set_parent
import kubriko.creator.generated.resources.error_command_parent_cycle
import kubriko.creator.generated.resources.error_command_parent_property_kind

@OptIn(EditorMutation::class)
internal object SetParent : WriteCommand<SetParent.Input> {
    override val name = "set_parent"
    override val description = "Makes an Actor type inherit from another one, like GameMaker's parent objects, or from none. It gets the parent's " +
        "Properties (a Property of its own with the same name only changes the initial value) and Events (an Event of its own of the same kind " +
        "replaces the parent's, and a runParentEvent Block runs the parent's there), and the parent stands for it in collision Events and " +
        "other Blocks that name an Actor type."
    override val title = Res.string.command_set_parent
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.actorType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateActorType(input.actorType) ?: input.parent?.let { parent ->
        val actorTypes = context.requireBlockTypes.actorTypes.value
        context.validateActorType(parent) ?: when {
            ActorTypeFamily.isAncestor(input.actorType, parent, actorTypes.associate { it.name to it.parent }) -> Rejection(Res.string.error_command_parent_cycle, parent, input.actorType)
            else -> {
                val inherited = ActorTypeFamily(actorTypes).propertiesOf(parent).associateBy { it.name }
                context.requireActorType(input.actorType).properties.firstOrNull { property ->
                    inherited[property.name]?.let { PropertyKind.of(it) != PropertyKind.of(property) } == true
                }?.let { Rejection(Res.string.error_command_parent_property_kind, it.name, parent) }
            }
        }
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireActorType(input.actorType)
        context.requireBlockTypes.put(definition.copy(parent = input.parent))
        return Applied(inverse = Invocation(SetParent, input.copy(parent = definition.parent)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Actor type.")
        @SerialName("actorType") val actorType: String,
        @Description("The name of the Actor type it inherits from, or none for no parent.")
        @SerialName("parent") val parent: String? = null,
    )
}
