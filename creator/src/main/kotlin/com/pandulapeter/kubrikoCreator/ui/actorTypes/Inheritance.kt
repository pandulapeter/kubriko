/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition

/**
 * What a type's editor shows about inheritance: the Properties and Events an Actor type gets from its parent (those it
 * doesn't replace), the Actor types it can have as a parent, and the ones that have it as theirs.
 *
 * @param propertyOwners The ancestor declaring each of [properties], by the Property's name.
 */
internal data class Inheritance(
    val properties: List<PropertyDefinition> = emptyList(),
    val propertyOwners: Map<String, String> = emptyMap(),
    val events: List<EventDefinition> = emptyList(),
    val parentChoices: List<String> = emptyList(),
    val children: List<String> = emptyList(),
) {
    companion object {

        /**
         * The inheritance of [definition] among [types]; nothing for a Manager type. The Events come from [types], so
         * [family] only has to match their names, parents, and Properties.
         */
        fun of(
            definition: BlockTypeDefinition,
            types: List<BlockTypeDefinition>,
            family: ActorTypeFamily = ActorTypeFamily(types.filterIsInstance<ActorTypeDefinition>()),
        ): Inheritance {
            if (definition !is ActorTypeDefinition) return Inheritance()
            val actorTypes = types.filterIsInstance<ActorTypeDefinition>()
            val descendants = family.descendantsOf(definition.name).toSet()
            val ownNames = definition.properties.map { it.name }.toSet()
            val byName = actorTypes.associateBy { it.name }
            val inheritedEvents = family.parentOf(definition.name)?.let { family.eventsOf(it, byName::get) }.orEmpty()
            val inheritedProperties = family.propertiesOf(definition.name).filter { it.name !in ownNames }
            val ancestors = family.ancestorsOf(definition.name)
            return Inheritance(
                properties = inheritedProperties,
                propertyOwners = inheritedProperties.mapNotNull { property ->
                    ancestors.firstOrNull { ancestor -> byName[ancestor]?.properties?.any { it.name == property.name } == true }?.let { property.name to it }
                }.toMap(),
                events = with(ActorTypeFamily) { inheritedEvents.filter { inherited -> definition.events.none { it.replaces(inherited) } } },
                parentChoices = actorTypes.map { it.name }.filter { it != definition.name && it !in descendants },
                children = actorTypes.filter { it.parent == definition.name }.map { it.name }.sorted(),
            )
        }
    }
}
