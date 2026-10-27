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

import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.withPropertyInserted
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
import kubriko.creator.generated.resources.command_add_property

@OptIn(EditorMutation::class)
internal object AddProperty : WriteCommand<AddProperty.Input> {
    override val name = "add_property"
    override val description = "Adds a Property to a block-built Actor type: a value each of its Actors keeps (like a score), editable per Actor in Scenes, " +
        "which the setNumber, changeNumber, setText, and setToggle Blocks change and the log Block can show."
    override val title = Res.string.command_add_property
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.ActorType(input.actorType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateActorType(input.actorType)
        ?: validateProperty(input.property, context.requireActorType(input.actorType).properties)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireActorType(input.actorType)
        context.requireActorTypes.put(definition.withPropertyInserted(input.index ?: definition.properties.size, input.property))
        context.changeSceneStates(input.actorType) { it.withoutMismatchedValue(input.property.name, PropertyKind.of(input.property)) }
        return Applied(inverse = Invocation(RemoveProperty, RemoveProperty.Input(input.actorType, input.property.name)))
    }

    @Serializable
    data class Input(
        @Description("The name of the Actor type.")
        @SerialName("actorType") val actorType: String,
        @Description("The Property to add.")
        @SerialName("property") val property: PropertyDefinition,
        @Description("Where the Property goes in the Actor type's list, starting from 0. Last if left out.")
        @SerialName("index") val index: Int? = null,
    )
}
