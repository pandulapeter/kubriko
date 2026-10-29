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
 * @param usesAudio Whether any type plays a sound or music, or stops the music, or any Scene has music.
 * @param sounds The sound files the Blocks play, by their path from `composeResources`.
 * @param music The music files the Blocks and the Scenes play, by their path from `composeResources`.
 * @param usesSprites Whether any Actor type is drawn as a sprite.
 */
internal data class PluginManagers(
    val collidableActorTypeNames: Set<String> = emptySet(),
    val usesKeyboard: Boolean = false,
    val usesAudio: Boolean = false,
    val sounds: Set<String> = emptySet(),
    val music: Set<String> = emptySet(),
    val usesSprites: Boolean = false,
) {
    val usesCollisions get() = collidableActorTypeNames.isNotEmpty()

    companion object {
        /**
         * @param sceneMusic The music of every Scene that has some, by its path from `composeResources`.
         */
        fun of(definitions: List<BlockTypeDefinition>, sceneMusic: Set<String> = emptySet()): PluginManagers {
            val actorTypes = definitions.filterIsInstance<ActorTypeDefinition>()
            val actorTypeNames = actorTypes.mapTo(mutableSetOf()) { it.name }
            val family = ActorTypeFamily(actorTypes)
            // A parent stands for its children, and children inherit their parent's Events.
            val collidableActorTypeNames = actorTypes.flatMapTo(mutableSetOf()) { definition ->
                family.eventsOf(definition.name)
                    .filter { it.type == EventType.COLLISION && it.otherActorType in actorTypeNames }
                    .flatMap { listOf(definition.name) + family.familyOf(it.otherActorType!!) }
            }
            val usesKeyboard = definitions.any { definition ->
                definition.events.any { event ->
                    (event.type == EventType.KEY_PRESSED && event.key?.isKnown == true) ||
                        event.blocks.any { (it as? BlockDefinition.If)?.condition is ConditionDefinition.KeyHeld }
                }
            }
            val blocks = definitions.flatMap { definition -> definition.events.flatMap { it.blocks } }
            return PluginManagers(
                collidableActorTypeNames = collidableActorTypeNames,
                usesKeyboard = usesKeyboard,
                usesAudio = sceneMusic.isNotEmpty() || BlockCodeGenerator.usesAudio(blocks),
                sounds = blocks.mapNotNullTo(sortedSetOf()) { (it as? BlockDefinition.PlaySound)?.audioFile },
                music = blocks.mapNotNullTo(sortedSetOf()) { (it as? BlockDefinition.PlayMusic)?.audioFile } + sceneMusic,
                usesSprites = actorTypes.any { definition -> definition.sprite?.let(SpriteFiles::accessorOf) != null },
            )
        }
    }
}
