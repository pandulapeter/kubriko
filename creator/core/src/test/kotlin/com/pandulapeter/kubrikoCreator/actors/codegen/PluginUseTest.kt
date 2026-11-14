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
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.EveryBlockTypes
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.SteeringKeys
import com.pandulapeter.kubrikoCreator.project.KubrikoArtifact
import kotlin.test.Test
import kotlin.test.assertEquals

class PluginUseTest {

    @Test
    fun eachUseNamesWhatNeedsThePluginOnce() {
        val ball = ActorTypeDefinition(
            name = "Ball",
            sprite = "drawable/ball.png",
            events = listOf(
                EventDefinition(EventType.COLLISION, otherActorType = "Paddle", blocks = listOf(BlockDefinition.PlaySound("files/sounds/hit.wav"))),
                EventDefinition(EventType.COLLISION, otherActorType = "Paddle", blocks = listOf(BlockDefinition.PlaySound("files/sounds/hit.wav"))),
                EventDefinition(EventType.COLLISION, otherActorType = "Deleted"),
            ),
        )
        val game = ManagerTypeDefinition(
            name = "Game",
            events = listOf(
                EventDefinition(EventType.KEY_PRESSED, key = KeyChoice.SPACE, blocks = listOf(BlockDefinition.StopMusic)),
                EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.If(ConditionDefinition.KeyHeld(KeyChoice.A)), BlockDefinition.PlayMusic("files/music/theme.mp3"), BlockDefinition.PlayMusic())),
            ),
        )
        assertEquals(
            listOf(
                PluginUse(KubrikoArtifact.COLLISION, PluginReason.COLLISION_EVENT, "Ball", "Paddle"),
                PluginUse(KubrikoArtifact.AUDIO_PLAYBACK, PluginReason.PLAYS_SOUND, "Ball", "files/sounds/hit.wav"),
                PluginUse(KubrikoArtifact.SPRITES, PluginReason.SPRITE, "Ball", "drawable/ball.png"),
                PluginUse(KubrikoArtifact.KEYBOARD_INPUT, PluginReason.KEY_PRESSED_EVENT, "Game", "Spacebar"),
                PluginUse(KubrikoArtifact.AUDIO_PLAYBACK, PluginReason.STOPS_MUSIC, "Game"),
                PluginUse(KubrikoArtifact.KEYBOARD_INPUT, PluginReason.KEY_HELD_CONDITION, "Game", "A"),
                PluginUse(KubrikoArtifact.AUDIO_PLAYBACK, PluginReason.PLAYS_MUSIC, "Game", "files/music/theme.mp3"),
                PluginUse(KubrikoArtifact.AUDIO_PLAYBACK, PluginReason.SCENE_MUSIC, "levels/1-1", "files/music/level.mp3"),
            ),
            PluginUse.of(listOf(ball, ActorTypeDefinition(name = "Paddle"), game), sceneMusic = mapOf("levels/1-1" to "files/music/level.mp3")),
        )
    }

    @Test
    fun whatNeedsNoPluginIsntAUse() {
        val paddle = ActorTypeDefinition(
            name = "Paddle",
            sprite = "files/paddle.png",
            events = listOf(EventDefinition(EventType.KEY_PRESSED), EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.PlaySound()))),
        )
        assertEquals(emptyList(), PluginUse.of(listOf(paddle)))
    }

    @Test
    fun movingWithKeysUsesTheKeyboardOnlyWhereItsCodeIsGenerated() {
        val hero = ActorTypeDefinition(
            name = "Hero",
            events = listOf(
                EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.MoveWithKeys())),
                EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.MoveWithKeys(), BlockDefinition.MoveWithKeys(keys = SteeringKeys.WASD))),
            ),
        )
        val game = ManagerTypeDefinition(name = "Game", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.MoveWithKeys()))))
        assertEquals(listOf(PluginUse(KubrikoArtifact.KEYBOARD_INPUT, PluginReason.MOVES_WITH_KEYS, "Hero")), PluginUse.of(listOf(hero, game)))
        assertEquals(emptyList(), PluginUse.of(listOf(hero.copy(events = hero.events.take(1)), game)))
    }

    @Test
    fun theIntegrationProjectUsesEveryPluginTheGeneratedCodeCanNeed() {
        assertEquals(
            setOf(KubrikoArtifact.AUDIO_PLAYBACK, KubrikoArtifact.COLLISION, KubrikoArtifact.KEYBOARD_INPUT, KubrikoArtifact.SPRITES),
            PluginUse.of(EveryBlockTypes.all).mapTo(HashSet()) { it.artifact },
        )
    }
}
