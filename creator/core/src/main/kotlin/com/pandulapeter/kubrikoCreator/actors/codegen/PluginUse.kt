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
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.actors.audioFile
import com.pandulapeter.kubrikoCreator.project.KubrikoArtifact

/**
 * One reason the game uses one of Kubriko's plugins: an Event, a Block, an Actor type's look, or a Scene that needs it.
 *
 * @param owner The type whose Event, Block, or look it is, or the Scene, for [PluginReason.SCENE_MUSIC].
 * @param subject What it names: the other Actor type, the key, or the file, if it names one.
 */
public data class PluginUse(
    val artifact: KubrikoArtifact,
    val reason: PluginReason,
    val owner: String,
    val subject: String? = null,
) {
    public companion object {

        /**
         * Every reason [definitions] and the Scenes in [sceneMusic] (the music file of each Scene that has some, by its
         * name) give the game to use a plugin, as the generated code decides them ([PluginManagers]), each named once.
         */
        public fun of(definitions: List<BlockTypeDefinition>, sceneMusic: Map<String, String> = emptyMap()): List<PluginUse> {
            val actorTypeNames = definitions.filterIsInstance<ActorTypeDefinition>().mapTo(HashSet()) { it.name }
            val uses = LinkedHashSet<PluginUse>()
            definitions.forEach { definition ->
                definition.events.forEach { event ->
                    when {
                        event.type == EventType.COLLISION && event.otherActorType in actorTypeNames && definition is ActorTypeDefinition ->
                            uses += PluginUse(KubrikoArtifact.COLLISION, PluginReason.COLLISION_EVENT, definition.name, event.otherActorType)
                        event.type == EventType.KEY_PRESSED && event.key?.isKnown == true ->
                            uses += PluginUse(KubrikoArtifact.KEYBOARD_INPUT, PluginReason.KEY_PRESSED_EVENT, definition.name, event.key.composeKeyName)
                    }
                    event.blocks.forEach { block -> useOf(block, definition.name)?.let(uses::add) }
                }
                if (definition is ActorTypeDefinition) {
                    definition.sprite?.takeIf { SpriteFiles.accessorOf(it) != null }?.let { sprite ->
                        uses += PluginUse(KubrikoArtifact.SPRITES, PluginReason.SPRITE, definition.name, sprite)
                    }
                }
            }
            sceneMusic.forEach { (scene, music) -> uses += PluginUse(KubrikoArtifact.AUDIO_PLAYBACK, PluginReason.SCENE_MUSIC, scene, music) }
            return uses.toList()
        }

        private fun useOf(block: BlockDefinition, owner: String) = when {
            block is BlockDefinition.If && block.condition is ConditionDefinition.KeyHeld ->
                PluginUse(KubrikoArtifact.KEYBOARD_INPUT, PluginReason.KEY_HELD_CONDITION, owner, (block.condition as ConditionDefinition.KeyHeld).key.composeKeyName)
            block is BlockDefinition.PlaySound && block.audioFile != null -> PluginUse(KubrikoArtifact.AUDIO_PLAYBACK, PluginReason.PLAYS_SOUND, owner, block.audioFile)
            block is BlockDefinition.PlayMusic && block.audioFile != null -> PluginUse(KubrikoArtifact.AUDIO_PLAYBACK, PluginReason.PLAYS_MUSIC, owner, block.audioFile)
            block == BlockDefinition.StopMusic -> PluginUse(KubrikoArtifact.AUDIO_PLAYBACK, PluginReason.STOPS_MUSIC, owner)
            else -> null
        }
    }
}
