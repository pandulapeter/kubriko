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

import com.pandulapeter.kubrikoCreator.actors.AnimatedValue
import com.pandulapeter.kubrikoCreator.actors.AnimationEasing
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
        assertEquals(
            "Move in a direction: a random number from 0 to 360 degrees at speed 120",
            BlockNotation.of(BlockDefinition.MoveInDirection(NumberValue.Random(0f, 360f), NumberValue.Fixed(120f))),
        )
        assertEquals("Set the color to #E05050", BlockNotation.of(BlockDefinition.SetColor()))
        assertEquals("Set the color to one of #20C060, #FFFFFF at random", BlockNotation.of(BlockDefinition.SetColor(listOf(0xFF20C060, 0xFFFFFFFF))))
        assertEquals("Set the opacity to a random number from 20 to 80 percent", BlockNotation.of(BlockDefinition.SetOpacity(NumberValue.Random(20f, 80f))))
        assertEquals("Change the opacity by -10 percent", BlockNotation.of(BlockDefinition.ChangeOpacity()))
        assertEquals("Animate the scale from 1 to 2 in 0.5 s, ending slowly", BlockNotation.of(BlockDefinition.Animate()))
        assertEquals(
            "Animate the opacity from 100 to 0 percent in a random number from 1 to 2 s, evenly, then remove this Actor",
            BlockNotation.of(BlockDefinition.Animate(AnimatedValue.Opacity(), NumberValue.Random(1f, 2f), AnimationEasing.LINEAR, removesActor = true)),
        )
        assertEquals("Animate the color from #FFFFFF to #102030 in 0.5 s, starting slowly", BlockNotation.of(BlockDefinition.Animate(AnimatedValue.Color(to = 0xFF102030), easing = AnimationEasing.EASE_IN)))
        assertEquals("Animate the position by (0, -100) in 0.5 s, starting and ending slowly", BlockNotation.of(BlockDefinition.Animate(AnimatedValue.Position(), easing = AnimationEasing.EASE_IN_OUT)))
        assertEquals(
            "Animate the position to (the left edge, 5) in 0.5 s, ending slowly",
            BlockNotation.of(BlockDefinition.Animate(AnimatedValue.Position(NumberValue.Edge(SceneEdge.LEFT), NumberValue.Fixed(5f), isRelative = false))),
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
