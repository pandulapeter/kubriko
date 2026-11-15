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
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.MusicValue
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.SoundValue
import com.pandulapeter.kubrikoCreator.project.KubrikoArtifact
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioPropertyCodeTest {
    private val properties = listOf(PropertyDefinition.Sound("cry", "files/sounds/hit.wav"), PropertyDefinition.Music("tune"))
    private val scope = BlockScope("Enemy", properties = properties)

    private fun line(block: BlockDefinition) = BlockCodeGenerator.blockCode(block, EventType.EVERY_FRAME, scope).lines.single()

    @Test
    fun aBlockPlaysWhatItsPropertyHoldsOrIsACommentWithoutOne() {
        assertEquals("generatedAudio.playSound(cry)", line(BlockDefinition.PlaySound(SoundValue.Property("cry"))))
        assertEquals("generatedAudio.playMusic(tune, isLooping = false)", line(BlockDefinition.PlayMusic(MusicValue.Property("tune"), isLooping = false)))
        assertEquals("// Choose a sound for this Block.", line(BlockDefinition.PlaySound(SoundValue.Property("missing"))))
        assertEquals("cry: String = \"files/sounds/hit.wav\"", "cry: String = ${BlockCodeGenerator.initialValueLiteral(properties.first())}")
    }

    @Test
    fun playingAPropertysSoundUsesTheAudioPluginAndItsStartingFileIsPreloaded() {
        val enemy = ActorTypeDefinition(name = "Enemy", properties = properties, events = listOf(EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.PlaySound(SoundValue.Property("cry"))))))
        assertEquals(listOf(PluginUse(KubrikoArtifact.AUDIO_PLAYBACK, PluginReason.PLAYS_SOUND_PROPERTY, "Enemy", "cry")), PluginUse.of(listOf(enemy)))
        val managers = PluginManagers.of(listOf(enemy))
        assertTrue(managers.usesAudio)
        assertEquals(setOf("files/sounds/hit.wav"), managers.sounds.toSet())
    }

    @Test
    fun aBlockReadingAPropertyThatIsntThereUsesNoPlugin() {
        val enemy = ActorTypeDefinition(name = "Enemy", events = listOf(EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.PlaySound(SoundValue.Property("cry"))))))
        assertEquals(emptyList(), PluginUse.of(listOf(enemy)))
        assertFalse(PluginManagers.of(listOf(enemy)).usesAudio)
    }

    @Test
    fun aBlockPlayingAManagersMusicNamesThePropertyWithItsManager() {
        val game = ManagerTypeDefinition(name = "Game", properties = listOf(PropertyDefinition.Music("tune", initialValue = "files/music/theme.mp3")))
        val enemy = ActorTypeDefinition(name = "Enemy", events = listOf(EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.PlayMusic(MusicValue.Property("tune", manager = "Game"))))))
        assertEquals(listOf(PluginUse(KubrikoArtifact.AUDIO_PLAYBACK, PluginReason.PLAYS_MUSIC_PROPERTY, "Enemy", "Game.tune")), PluginUse.of(listOf(game, enemy)))
    }

    @Test
    fun aChildPlayingItsParentsSoundPropertyUsesTheAudioPluginThroughTheFamilyGiven() {
        val enemy = ActorTypeDefinition(name = "Enemy", properties = properties)
        val boss = ActorTypeDefinition(name = "Boss", parent = "Enemy", events = listOf(EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.PlaySound(SoundValue.Property("cry"))))))
        val expected = listOf(PluginUse(KubrikoArtifact.AUDIO_PLAYBACK, PluginReason.PLAYS_SOUND_PROPERTY, "Boss", "cry"))
        assertEquals(expected, PluginUse.of(listOf(enemy, boss)))
        assertEquals(expected, PluginUse.of(listOf(enemy, boss), family = ActorTypeFamily(listOf(enemy, boss))))
        assertTrue(PluginManagers.of(listOf(enemy, boss)).usesAudio)
    }
}
