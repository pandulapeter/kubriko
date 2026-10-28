/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

/**
 * What the generated code needs from Kubriko's plugins, decided from every type of the project at once, so that a game
 * only depends on the plugins it uses.
 *
 * @param collidableActorTypeNames The Actor types that take part in a collision Event, on either side of it: only these
 *   get a collision mask.
 * @param usesKeyboard Whether any type has a key pressed Event or checks whether a key is held.
 */
internal data class PluginManagers(
    val collidableActorTypeNames: Set<String> = emptySet(),
    val usesKeyboard: Boolean = false,
) {
    val usesCollisions get() = collidableActorTypeNames.isNotEmpty()

    companion object {
        fun of(definitions: List<BlockTypeDefinition>): PluginManagers {
            val actorTypes = definitions.filterIsInstance<ActorTypeDefinition>()
            val actorTypeNames = actorTypes.mapTo(mutableSetOf()) { it.name }
            val collidableActorTypeNames = actorTypes.flatMapTo(mutableSetOf()) { definition ->
                definition.events
                    .filter { it.type == EventType.COLLISION && it.otherActorType in actorTypeNames }
                    .flatMap { listOf(definition.name, it.otherActorType!!) }
            }
            val usesKeyboard = definitions.any { definition ->
                definition.events.any { event ->
                    (event.type == EventType.KEY_PRESSED && event.key != null) ||
                        event.blocks.any { (it as? BlockDefinition.If)?.condition is ConditionDefinition.KeyHeld }
                }
            }
            return PluginManagers(collidableActorTypeNames, usesKeyboard)
        }
    }
}
