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
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.project.KubrikoArtifact
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeneratedCodeTest {

    @Test
    fun aBlockIsGeneratedExactlyWhereItsCodeIsntAComment() = BlockTypeKind.entries.forEach { kind ->
        val scope = BlockScope("Type", kind)
        BlockKind.entries.forEach { blockKind ->
            EventType.entries.forEach { eventType ->
                val block = blockKind.createDefault()
                val lines = BlockCodeGenerator.blockCode(block, eventType, scope).lines
                val isLeftOut = lines == listOf("// This Block only works in an Actor type.") || blockKind.misplacedComment?.let { lines == listOf(it) } == true
                assertEquals(!isLeftOut, GeneratedCode.isGenerated(block, eventType, kind), "$blockKind in $eventType of a $kind")
            }
        }
    }

    @Test
    fun anEventIsGeneratedOnlyInTheTypesThatHaveItAndWithWhatItNamesExisting() {
        val actorTypes = listOf("Ball")
        fun actor(event: EventDefinition) = GeneratedCode.isGenerated(event, BlockTypeKind.ACTOR, actorTypes)
        fun manager(event: EventDefinition) = GeneratedCode.isGenerated(event, BlockTypeKind.MANAGER, actorTypes)
        assertTrue(actor(EventDefinition(EventType.COLLISION, otherActorType = "Ball")))
        assertFalse(actor(EventDefinition(EventType.COLLISION, otherActorType = "Deleted")))
        assertFalse(manager(EventDefinition(EventType.COLLISION, otherActorType = "Ball")))
        assertFalse(actor(EventDefinition(EventType.GAME_STARTED)))
        assertTrue(manager(EventDefinition(EventType.GAME_STARTED)))
        assertFalse(manager(EventDefinition(EventType.ADDED_TO_SCENE)))
        assertTrue(actor(EventDefinition(EventType.KEY_PRESSED, key = KeyChoice.SPACE)))
        assertFalse(actor(EventDefinition(EventType.KEY_PRESSED, key = KeyChoice("NoSuchKey"))))
        assertFalse(manager(EventDefinition(EventType.TIMER_FINISHED, timer = "not a name")))
        assertTrue(manager(EventDefinition(EventType.EVERY_FRAME)))
    }

    @Test
    fun whatIsLeftOutOfTheCodeNeedsNoPlugin() {
        val sound = BlockDefinition.PlaySound("files/sounds/hit.wav")
        val ball = ActorTypeDefinition(
            name = "Ball",
            events = listOf(
                EventDefinition(EventType.COLLISION, otherActorType = "Deleted", blocks = listOf(sound)),
                EventDefinition(EventType.GAME_STARTED, blocks = listOf(BlockDefinition.MoveWithKeys(), sound)),
                EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.If(ConditionDefinition.KeyHeld(KeyChoice("NoSuchKey"))))),
            ),
        )
        val game = ManagerTypeDefinition(name = "Game", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.MoveWithKeys()))))
        assertEquals(emptyList(), PluginUse.of(listOf(ball, game)))
        assertEquals(PluginManagers(), PluginManagers.of(listOf(ball, game)))
        val playing = PluginManagers.of(listOf(ball.copy(events = ball.events + EventDefinition(EventType.EVERY_FRAME, blocks = listOf(sound)))))
        assertEquals(setOf("files/sounds/hit.wav"), playing.sounds)
        assertTrue(playing.usesAudio)
        assertEquals(setOf(KubrikoArtifact.AUDIO_PLAYBACK), PluginUse.of(listOf(ball.copy(events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(sound)))))).mapTo(HashSet()) { it.artifact })
    }
}
