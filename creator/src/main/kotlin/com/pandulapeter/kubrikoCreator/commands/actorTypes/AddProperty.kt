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
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.withPropertyInserted
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.CompositeCommand
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.commands.scenes.SetActorsState
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.command_add_property

@OptIn(EditorMutation::class)
internal object AddProperty : WriteCommand<AddProperty.Input> {
    override val name = "add_property"
    override val description = "Adds a Property to a block-built Actor type or Manager type. In an Actor type, it is a value each of its Actors keeps " +
        "(like its health), editable per Actor in Scenes; in a Manager type, a game-wide value (like the score) that the Blocks of every type can use. " +
        "The setNumber, changeNumber, setText, and setToggle Blocks change it, and log Blocks and a Manager's texts can show it."
    override val title = Res.string.command_add_property
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.actorType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateType(input.actorType)
        ?: validateProperty(input.property, context.requireType(input.actorType).properties)
        ?: context.validateInheritedKind(input.actorType, input.property)

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireType(input.actorType)
        context.requireBlockTypes.put(definition.withPropertyInserted(input.index ?: definition.properties.size, input.property))
        val replaced = try {
            val family = ActorTypeFamily(context.requireBlockTypes.actorTypes.value).familyOf(input.actorType).toSet()
            context.changeSceneStates(family) { it.withoutMismatchedValue(input.property.name, PropertyKind.of(input.property)) }
        } catch (exception: Exception) {
            context.requireBlockTypes.put(definition)
            throw exception
        }
        val removal = Invocation(RemoveProperty, RemoveProperty.Input(input.actorType, input.property.name))
        if (replaced.isEmpty()) return Applied(inverse = removal)
        val restoredStates = replaced.map { (scene, states) ->
            Invocation(SetActorsState, SetActorsState.Input(scene, states.map { (id, state) -> SetActorsState.ActorState(id, state) }), isRestoring = true)
        }
        return Applied(inverse = Invocation(CompositeCommand, CompositeCommand.Input(listOf(removal.copy(isRestoring = true)) + restoredStates)))
    }

    @Serializable
    data class Input(
        @Description(Descriptions.TYPE_NAME)
        @SerialName("actorType") val actorType: String,
        @Description("The Property to add.")
        @SerialName("property") val property: PropertyDefinition,
        @Description("Where the Property goes in the Actor type's list, starting from 0. Last if left out.")
        @SerialName("index") val index: Int? = null,
    )
}
