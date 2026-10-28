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
 * doesn't replace), and the Actor types it can have as a parent.
 */
internal data class Inheritance(
    val properties: List<PropertyDefinition> = emptyList(),
    val events: List<EventDefinition> = emptyList(),
    val parentChoices: List<String> = emptyList(),
) {
    companion object {

        /**
         * The inheritance of [definition] among [types]; nothing for a Manager type.
         */
        fun of(definition: BlockTypeDefinition, types: List<BlockTypeDefinition>): Inheritance {
            if (definition !is ActorTypeDefinition) return Inheritance()
            val actorTypes = types.filterIsInstance<ActorTypeDefinition>()
            val family = ActorTypeFamily(actorTypes)
            val ownNames = definition.properties.map { it.name }.toSet()
            val inheritedEvents = family.parentOf(definition.name)?.let(family::eventsOf).orEmpty()
            return Inheritance(
                properties = family.propertiesOf(definition.name).filter { it.name !in ownNames },
                events = with(ActorTypeFamily) { inheritedEvents.filter { inherited -> definition.events.none { it.replaces(inherited) } } },
                parentChoices = actorTypes.map { it.name }.filter { it != definition.name && it !in family.descendantsOf(definition.name) },
            )
        }
    }
}
