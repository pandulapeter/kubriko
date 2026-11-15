/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.SceneEdge
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddBlock
import com.pandulapeter.kubrikoCreator.context.BlockNotation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AudioVolumeTest {
    private val ball = ActorTypeDefinition(name = "Ball", events = listOf(EventDefinition(EventType.EVERY_FRAME)))

    @Test
    fun aVolumeIsAPercentAndNoEdge() = runTest {
        val context = commandContext(ball)
        fun rejection(block: BlockDefinition) = AddBlock.validate(context, AddBlock.Input("Ball", 0, block))?.reason
        assertNull(rejection(BlockDefinition.PlaySound(volume = NumberValue.Random(0f, 100f))))
        assertEquals(CommandProblem.VOLUME_OUT_OF_RANGE, rejection(BlockDefinition.PlaySound(volume = NumberValue.Fixed(150f))))
        assertEquals(CommandProblem.VOLUME_OUT_OF_RANGE, rejection(BlockDefinition.PlayMusic(volume = NumberValue.Random(-10f, 50f))))
        assertEquals(CommandProblem.NUMBER_EDGE_NOT_TAKEN, rejection(BlockDefinition.PlayMusic(volume = NumberValue.Edge(SceneEdge.LEFT))))
    }

    @Test
    fun aVolumeOtherThanFullReadsInTheSentence() {
        assertEquals("Play sound hit", BlockNotation.of(BlockDefinition.PlaySound("hit")))
        assertEquals("Play music theme, once, at 40%", BlockNotation.of(BlockDefinition.PlayMusic("theme", isLooping = false, volume = NumberValue.Fixed(40f))))
    }
}
