/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockKind
import com.pandulapeter.kubrikoCreator.actors.Comparison
import com.pandulapeter.kubrikoCreator.actors.ConditionDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.SceneEdge
import kotlin.test.Test
import kotlin.test.assertEquals

class BlockNotationTest {

    @Test
    fun everyBlockHasALineOfItsOwn() {
        val lines = BlockKind.entries.map { BlockNotation.of(it.createDefault()) }
        assertEquals(lines.size, lines.toSet().size, lines.joinToString("\n"))
    }

    @Test
    fun blocksReadAsTheEditorShowsThem() {
        assertEquals("Change score of Game by 10", BlockNotation.of(BlockDefinition.ChangeNumber("score", NumberValue.Fixed(10f), manager = "Game")))
        assertEquals("Set velocity to (200, -0.5)", BlockNotation.of(BlockDefinition.SetVelocity(NumberValue.Fixed(200f), NumberValue.Fixed(-0.5f))))
        assertEquals("Add an Actor of (none) at (0, 0) from this Actor", BlockNotation.of(BlockDefinition.AddActor()))
        assertEquals("Start Timer spawn for 2 s, repeating", BlockNotation.of(BlockDefinition.StartTimer("spawn", NumberValue.Fixed(2f), isRepeating = true)))
        assertEquals("If lives of Game is at most 0", BlockNotation.of(BlockDefinition.If(ConditionDefinition.Number("lives", Comparison.LESS_OR_EQUAL, NumberValue.Fixed(0f), "Game"))))
        assertEquals("Write to the console \"Say \\\"hi\\\"\\n\"", BlockNotation.of(BlockDefinition.Log("Say \"hi\"\n")))
        assertEquals(
            "Set velocity to (a random number from -50 to 50, speed of Game)",
            BlockNotation.of(BlockDefinition.SetVelocity(NumberValue.Random(-50f, 50f), NumberValue.Property("speed", manager = "Game"))),
        )
        assertEquals("Repeat a random whole number from 1 to 3 times", BlockNotation.of(BlockDefinition.Repeat(NumberValue.Random(1f, 3f, isWhole = true))))
        assertEquals("If by chance ((none)%)", BlockNotation.of(BlockDefinition.If(ConditionDefinition.Chance(NumberValue.Property()))))
        assertEquals(
            "Add an Actor of Drifter at (the right edge + 50, 10) from this Actor (an edge is the Scene's own)",
            BlockNotation.of(BlockDefinition.AddActor("Drifter", NumberValue.Edge(SceneEdge.RIGHT, 50f), NumberValue.Fixed(10f))),
        )
        assertEquals(
            "Add an Actor of Drifter at (the right edge + 50, a random number from the top edge - 0.5 to the bottom edge) from the center",
            BlockNotation.of(
                BlockDefinition.AddActor("Drifter", NumberValue.Edge(SceneEdge.RIGHT, 50f), NumberValue.BetweenEdges(SceneEdge.TOP, -0.5f, SceneEdge.BOTTOM), isRelative = false),
            ),
        )
    }

    @Test
    fun anEventListsItsBlocksIndexedAndIndented() {
        val event = EventDefinition(
            type = EventType.COLLISION,
            otherActorType = "Brick",
            blocks = listOf(
                BlockDefinition.RemoveOther,
                BlockDefinition.If(ConditionDefinition.KeyHeld(KeyChoice.SPACE)),
                BlockDefinition.ChangeNumber("score", NumberValue.Fixed(10f), manager = "Game"),
                BlockDefinition.End,
            ),
        )
        assertEquals(
            """
            When it collides with Brick
              0 Remove the other Actor
              1 If Spacebar is held
                2 Change score of Game by 10
              3 End
            """.trimIndent(),
            BlockNotation.withBlocks(event),
        )
        assertEquals("When Timer (none) finishes", BlockNotation.of(EventDefinition(EventType.TIMER_FINISHED)))
    }
}
