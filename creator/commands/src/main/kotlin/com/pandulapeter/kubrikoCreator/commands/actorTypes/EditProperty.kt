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
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import com.pandulapeter.kubrikoCreator.actors.PropertyNaming
import com.pandulapeter.kubrikoCreator.actors.withManagerPropertyRenamed
import com.pandulapeter.kubrikoCreator.actors.withPropertyReplaced
import com.pandulapeter.kubrikoCreator.actors.withPropertyUsesRenamed
import com.pandulapeter.kubrikoCreator.commands.Applied
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.commands.Descriptions
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.WriteCommand
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.schema.Description
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Renames a Property or changes its initial value. Its kind stays, since the Scenes keep values of that kind.
 */
@OptIn(EditorMutation::class)
public object EditProperty : WriteCommand<EditProperty.Input> {
    override val name: String = "edit_property"
    override val description: String = "Renames a Property of a block-built Actor type or Manager type, or changes its initial value. Renaming also updates the Blocks " +
        "and texts that use it (in every Actor type that inherits it, or in every type, for a Manager type's Property) and the values Actors keep in Scenes. " +
        "A Property an Actor type inherits is renamed in the type it comes from. Its kind (number, text, or toggle) can't change: remove it and add a new one instead."
    override val inputSerializer: KSerializer<Input> = Input.serializer()

    override fun target(context: CommandContext, input: Input): CommandTarget? = CommandTarget.BlockType(input.actorType)

    override fun validate(context: CommandContext, input: Input): Rejection? = context.validateProperty(input.actorType, input.property)
        ?: context.requireType(input.actorType).let { definition ->
            if (PropertyKind.of(definition.properties.first { it.name == input.property }) != PropertyKind.of(input.newProperty)) {
                Rejection(CommandProblem.PROPERTY_KIND_CHANGED)
            } else {
                validateProperty(input.newProperty, definition.properties, replacedName = input.property)
                    ?: if (definition is ActorTypeDefinition && input.newProperty.name != input.property) context.validateFamilyRename(definition.name, input.property, input.newProperty.name) else null
            }
        }

    /**
     * Why the Actor type [actorType] can't rename its Property [oldName] to [newName]: it only inherits it, or a type of the
     * family that renaming changes already has a Property called [newName].
     */
    private fun CommandContext.validateFamilyRename(actorType: String, oldName: String, newName: String): Rejection? {
        val family = ActorTypeFamily(requireBlockTypes.actorTypes.value)
        val declarer = family.declarerOf(actorType, oldName)
        if (declarer != null && declarer != actorType) return Rejection(CommandProblem.PROPERTY_INHERITED, oldName, declarer)
        return family.familyOf(actorType).firstNotNullOfOrNull { member ->
            val names = (family.propertiesOf(member) + requireActorType(member).properties).map { it.name }.filter { it != oldName }
            PropertyNaming.validate(newName, names)?.let(::Rejection)
        }
    }

    override suspend fun apply(context: CommandContext, input: Input): Applied {
        val definition = context.requireType(input.actorType)
        val previous = definition.properties.first { it.name == input.property }
        val previousTypes = LinkedHashMap<String, BlockTypeDefinition>()
        fun put(type: BlockTypeDefinition, changed: BlockTypeDefinition) {
            if (changed == type) return
            previousTypes.putIfAbsent(type.name, type)
            context.requireBlockTypes.put(changed)
        }
        put(definition, definition.withPropertyReplaced(input.property, input.newProperty))
        if (input.newProperty.name != input.property) {
            val sceneTypes = if (definition is ManagerTypeDefinition) {
                context.requireBlockTypes.types.value.forEach { type -> put(type, type.withManagerPropertyRenamed(definition.name, input.property, input.newProperty.name)) }
                setOf(definition.name)
            } else {
                renameInDescendants(context, input, ::put)
            }
            try {
                context.changeSceneStates(sceneTypes) { it.withValueRenamed(input.property, input.newProperty.name) }
            } catch (exception: Exception) {
                previousTypes.values.forEach(context.requireBlockTypes::put)
                throw exception
            }
        }
        return Applied(inverse = Invocation(EditProperty, Input(input.actorType, input.newProperty.name, previous)))
    }

    /**
     * Renames the Property the Actor type of [input] declares in the types that inherit it: a redeclaration keeps its
     * initial value, and the Blocks and texts using it follow.
     *
     * @return The names of the whole family, whose Actors' Scene values follow.
     */
    private fun renameInDescendants(context: CommandContext, input: Input, put: (BlockTypeDefinition, BlockTypeDefinition) -> Unit): Set<String> {
        val family = ActorTypeFamily(context.requireBlockTypes.actorTypes.value)
        val kind = PropertyKind.of(input.newProperty)
        family.descendantsOf(input.actorType).forEach { name ->
            val type = context.requireActorType(name)
            val redeclared = type.properties.firstOrNull { it.name == input.property && PropertyKind.of(it) == kind }
            put(type, if (redeclared == null) type.withPropertyUsesRenamed(input.property, input.newProperty.name) else type.withPropertyReplaced(input.property, redeclared.withName(input.newProperty.name)))
        }
        return family.familyOf(input.actorType).toSet()
    }

    /**
     * Typing into one Property's field: each change names the Property as the one before left it.
     */
    override fun mergesWith(previous: Input, next: Input): Boolean = next.actorType == previous.actorType && next.property == previous.newProperty.name

    override fun mergeReverts(first: Input, latest: Input): EditProperty.Input = first.copy(property = latest.property)

    @Serializable
    public data class Input(
        @Description(Descriptions.TYPE_NAME)
        @SerialName("actorType") val actorType: String,
        @Description("The current name of the Property.")
        @SerialName("property") val property: String,
        @Description("The Property as it should be: the same kind, with a new name or initial value.")
        @SerialName("newProperty") val newProperty: PropertyDefinition,
    )
}
