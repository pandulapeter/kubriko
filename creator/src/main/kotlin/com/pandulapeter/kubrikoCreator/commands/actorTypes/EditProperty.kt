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

import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.withManagerPropertyRenamed
import com.pandulapeter.kubrikoCreator.actors.withPropertyReplaced
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
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
    override val description = "Renames a Property of a block-built Actor type or Manager type, or changes its initial value. Renaming also updates the Blocks " +
        "and texts that use it (in every type, for a Manager type's Property) and the values Actors keep in Scenes. Its kind (number, text, or toggle) can't change: remove it and add a new one instead."
    override val title = Res.string.command_edit_property
    override val inputSerializer = Input.serializer()

    override fun target(context: CommandContext, input: Input) = CommandTarget.BlockType(input.actorType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateProperty(input.actorType, input.property)
        ?: context.requireType(input.actorType).properties.let { properties ->
            if (PropertyKind.of(properties.first { it.name == input.property }) != PropertyKind.of(input.newProperty)) {
                Rejection(Res.string.error_command_property_kind_changed)
            } else {
                validateProperty(input.newProperty, properties, replacedName = input.property)
            }
        }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireType(input.actorType)
        val previous = definition.properties.first { it.name == input.property }
        val previousTypes = linkedMapOf(definition.name to definition)
        context.requireBlockTypes.put(definition.withPropertyReplaced(input.property, input.newProperty))
        if (definition is ManagerTypeDefinition && input.newProperty.name != input.property) {
            context.requireBlockTypes.types.value.forEach { type ->
                val renamed = type.withManagerPropertyRenamed(definition.name, input.property, input.newProperty.name)
                if (renamed != type) {
                    previousTypes.putIfAbsent(type.name, type)
                    context.requireBlockTypes.put(renamed)
                }
            }
        }
        if (input.newProperty.name != input.property) {
            try {
                context.changeSceneStates(input.actorType) { it.withValueRenamed(input.property, input.newProperty.name) }
            } catch (exception: Exception) {
                previousTypes.values.forEach(context.requireBlockTypes::put)
                throw exception
            }
        }
        return Applied(inverse = Invocation(EditProperty, Input(input.actorType, input.newProperty.name, previous)))
    }

    /**
     * Typing into one Property's field: each change names the Property as the one before left it.
     */
    override fun mergesWith(previous: Input, next: Input) = next.actorType == previous.actorType && next.property == previous.newProperty.name

    override fun mergeReverts(first: Input, latest: Input) = first.copy(property = latest.property)

    @Serializable
    data class Input(
        @Description(Descriptions.TYPE_NAME)
        @SerialName("actorType") val actorType: String,
        @Description("The current name of the Property.")
        @SerialName("property") val property: String,
        @Description("The Property as it should be: the same kind, with a new name or initial value.")
        @SerialName("newProperty") val newProperty: PropertyDefinition,
    )
}
