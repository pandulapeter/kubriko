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
import com.pandulapeter.kubrikoCreator.actors.withPropertyReplaced
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
import kubriko.creator.generated.resources.command_edit_property
import kubriko.creator.generated.resources.error_command_property_kind_changed

/**
 * Renames a Property or changes its initial value. Its kind stays, since the Scenes keep values of that kind.
 */
@OptIn(EditorMutation::class)
internal object EditProperty : WriteCommand<EditProperty.Input> {
    override val name = "edit_property"
    override val description = "Renames a Property of a block-built Actor type or changes its initial value. Renaming also updates the Blocks that use it " +
        "and the values Actors keep in Scenes. Its kind (number, text, or toggle) can't change: remove it and add a new one instead."
    override val title = Res.string.command_edit_property
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.ActorType(input.actorType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateProperty(input.actorType, input.property)
        ?: context.requireActorType(input.actorType).properties.let { properties ->
            if (PropertyKind.of(properties.first { it.name == input.property }) != PropertyKind.of(input.newProperty)) {
                Rejection(Res.string.error_command_property_kind_changed)
            } else {
                validateProperty(input.newProperty, properties, replacedName = input.property)
            }
        }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireActorType(input.actorType)
        val previous = definition.properties.first { it.name == input.property }
        context.requireActorTypes.put(definition.withPropertyReplaced(input.property, input.newProperty))
        if (input.newProperty.name != input.property) {
            context.changeSceneStates(input.actorType) { it.withValueRenamed(input.property, input.newProperty.name) }
        }
        return Applied(inverse = Invocation(EditProperty, Input(input.actorType, input.newProperty.name, previous)))
    }

    override fun mergeKey(input: Input) = input.actorType to name

    @Serializable
    data class Input(
        @Description("The name of the Actor type.")
        @SerialName("actorType") val actorType: String,
        @Description("The current name of the Property.")
        @SerialName("property") val property: String,
        @Description("The Property as it should be: the same kind, with a new name or initial value.")
        @SerialName("newProperty") val newProperty: PropertyDefinition,
    )
}
