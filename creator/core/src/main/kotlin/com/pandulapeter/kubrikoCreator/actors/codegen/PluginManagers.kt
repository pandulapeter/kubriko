/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ActorTypeFamily
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.actors.audioFile
import com.pandulapeter.kubrikoCreator.project.KubrikoArtifact

/**
 * What the generated code needs from Kubriko's plugins, decided from every type of the project at once ([PluginUse]), so
 * that a game only depends on the plugins it uses.
 *
 * @param collidableActorTypeNames The Actor types that take part in a collision Event, on either side of it: only these
 *   get a collision mask.
 * @param usesKeyboard Whether any type has a key pressed Event or checks whether a key is held.
 * @param usesAudio Whether any type plays a sound or music, or stops the music, or any Scene has music.
 * @param sounds The sound files the Blocks play, by their path from `composeResources`.
 * @param music The music files the Blocks and the Scenes play, by their path from `composeResources`.
 * @param sprites The resource accessors of the sprites the Actor types are drawn as ([SpritesCodeGenerator]); the game uses
 *   the sprites plugin when there are any.
 */
internal data class PluginManagers(
    val collidableActorTypeNames: Set<String> = emptySet(),
    val usesKeyboard: Boolean = false,
    val usesAudio: Boolean = false,
    val sounds: Set<String> = emptySet(),
    val music: Set<String> = emptySet(),
    val sprites: Set<String> = emptySet(),
) {
    val usesCollisions get() = collidableActorTypeNames.isNotEmpty()
    val usesSprites get() = sprites.isNotEmpty()

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
            val pluginsInUse = PluginUse.of(definitions).mapTo(HashSet()) { it.artifact }
            val blocks = definitions.flatMap { definition -> definition.events.flatMap { it.blocks } }
            return PluginManagers(
                collidableActorTypeNames = collidableActorTypeNames,
                usesKeyboard = KubrikoArtifact.KEYBOARD_INPUT in pluginsInUse,
                usesAudio = sceneMusic.isNotEmpty() || KubrikoArtifact.AUDIO_PLAYBACK in pluginsInUse,
                sounds = blocks.mapNotNullTo(sortedSetOf()) { (it as? BlockDefinition.PlaySound)?.audioFile },
                music = blocks.mapNotNullTo(sortedSetOf()) { (it as? BlockDefinition.PlayMusic)?.audioFile } + sceneMusic,
                sprites = actorTypes.mapNotNullTo(sortedSetOf()) { type -> type.sprite?.let(SpriteFiles::accessorOf) },
            )
        }
    }
}
