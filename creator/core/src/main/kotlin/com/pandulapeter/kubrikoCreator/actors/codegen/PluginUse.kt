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
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.PropertyReference
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.actors.audioFile
import com.pandulapeter.kubrikoCreator.actors.leaves
import com.pandulapeter.kubrikoCreator.actors.propertyReference
import com.pandulapeter.kubrikoCreator.project.KubrikoArtifact

/**
 * One reason the game uses one of Kubriko's plugins: an Event, a Block, an Actor type's look, or a Scene that needs it.
 *
 * @param owner The type whose Event, Block, or look it is, or the Scene, for [PluginReason.SCENE_MUSIC].
 * @param subject What it names: the other Actor type, the key, the file, or the Property holding the file (`Score.theme` for
 *   a Manager type's), if it names one.
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
            val family = ActorTypeFamily(definitions.filterIsInstance<ActorTypeDefinition>())
            val uses = LinkedHashSet<PluginUse>()
            definitions.forEach { definition ->
                val scope by lazy { BlockScope.of(definition, definitions, family = family) }
                definition.events.filter { GeneratedCode.isGenerated(it, definition.kind, actorTypeNames) }.forEach { event ->
                    when {
                        event.type == EventType.COLLISION && event.otherActorType in actorTypeNames && definition is ActorTypeDefinition ->
                            uses += PluginUse(KubrikoArtifact.COLLISION, PluginReason.COLLISION_EVENT, definition.name, event.otherActorType)
                        event.type.takesKey && event.key?.isKnown == true ->
                            uses += PluginUse(KubrikoArtifact.KEYBOARD_INPUT, PluginReason.KEY_EVENT, definition.name, event.key.composeKeyName)
                    }
                    event.blocks.forEach { block ->
                        if (GeneratedCode.isGenerated(block, event.type, definition.kind)) {
                            useOf(block, definition.name) { scope }?.let(uses::add)
                            (block as? BlockDefinition.If)?.condition?.leaves?.forEach { condition -> useOf(condition, definition.name)?.let(uses::add) }
                        }
                    }
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

        private fun useOf(condition: ConditionDefinition, owner: String) = (condition as? ConditionDefinition.KeyHeld)?.key?.takeIf { it.isKnown }?.let { key ->
            PluginUse(KubrikoArtifact.KEYBOARD_INPUT, PluginReason.KEY_HELD_CONDITION, owner, key.composeKeyName)
        }

        /**
         * The use of a Block, whose [scope] is only built when it reads a Property.
         */
        private fun useOf(block: BlockDefinition, owner: String, scope: () -> BlockScope) = when (block) {
            is BlockDefinition.PlaySound -> audioUseOf(block.audioFile, block.sound.propertyReference, owner, PluginReason.PLAYS_SOUND, PluginReason.PLAYS_SOUND_PROPERTY, scope)
            is BlockDefinition.PlayMusic -> audioUseOf(block.audioFile, block.music.propertyReference, owner, PluginReason.PLAYS_MUSIC, PluginReason.PLAYS_MUSIC_PROPERTY, scope)
            else -> useOf(block, owner)
        }

        /**
         * The use of a Block playing [file], or the file its [property] holds if that Property exists, as the generated code
         * plays one ([AudioBlockCode]).
         */
        private fun audioUseOf(
            file: String?,
            property: PropertyReference?,
            owner: String,
            fileReason: PluginReason,
            propertyReason: PluginReason,
            scope: () -> BlockScope,
        ) = when {
            file != null -> PluginUse(KubrikoArtifact.AUDIO_PLAYBACK, fileReason, owner, file)
            property != null && scope().hasProperty(property) ->
                PluginUse(KubrikoArtifact.AUDIO_PLAYBACK, propertyReason, owner, property.manager?.let { "$it.${property.name}" } ?: property.name)
            else -> null
        }

        private fun useOf(block: BlockDefinition, owner: String) = when {
            block is BlockDefinition.MoveWithKeys -> PluginUse(KubrikoArtifact.KEYBOARD_INPUT, PluginReason.MOVES_WITH_KEYS, owner)
            block == BlockDefinition.StopMusic -> PluginUse(KubrikoArtifact.AUDIO_PLAYBACK, PluginReason.STOPS_MUSIC, owner)
            else -> null
        }
    }
}
