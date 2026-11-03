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
        assertEquals("Change score of Game by 10", BlockNotation.of(BlockDefinition.ChangeNumber("score", 10f, manager = "Game")))
        assertEquals("Set velocity to (200, -0.5)", BlockNotation.of(BlockDefinition.SetVelocity(200f, -0.5f)))
        assertEquals("Add an Actor of (none) at (0, 0) from this Actor", BlockNotation.of(BlockDefinition.AddActor()))
        assertEquals("Start Timer spawn for 2 s, repeating", BlockNotation.of(BlockDefinition.StartTimer("spawn", 2f, isRepeating = true)))
        assertEquals("If lives of Game is at most 0", BlockNotation.of(BlockDefinition.If(ConditionDefinition.Number("lives", Comparison.LESS_OR_EQUAL, 0f, "Game"))))
        assertEquals("Write to the console \"Say \\\"hi\\\"\\n\"", BlockNotation.of(BlockDefinition.Log("Say \"hi\"\n")))
    }

    @Test
    fun anEventListsItsBlocksIndexedAndIndented() {
        val event = EventDefinition(
            type = EventType.COLLISION,
            otherActorType = "Brick",
            blocks = listOf(
                BlockDefinition.RemoveOther,
                BlockDefinition.If(ConditionDefinition.KeyHeld(KeyChoice.SPACE)),
                BlockDefinition.ChangeNumber("score", 10f, manager = "Game"),
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
