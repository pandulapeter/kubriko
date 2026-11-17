/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import com.pandulapeter.kubrikoCreator.actors.BlockNesting.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class BlockNestingTest {
    private val ifBlock = BlockDefinition.If()
    private val repeat = BlockDefinition.Repeat()
    private val stop = BlockDefinition.Stop

    @Test
    fun sectionsNestAndElseAndEndStandAtTheDepthOfWhatOpenedThem() {
        val (nesting, openSections) = BlockNesting.of(listOf(ifBlock, repeat, stop, BlockDefinition.End, BlockDefinition.Else, stop, BlockDefinition.End, stop))
        assertEquals(
            listOf(
                BlockNesting(0, Role.OPENS),
                BlockNesting(1, Role.OPENS),
                BlockNesting(2, Role.PLAIN),
                BlockNesting(1, Role.CLOSES),
                BlockNesting(0, Role.ELSE),
                BlockNesting(1, Role.PLAIN),
                BlockNesting(0, Role.CLOSES),
                BlockNesting(0, Role.PLAIN),
            ),
            nesting,
        )
        assertEquals(0, openSections)
    }

    @Test
    fun elseAndEndWithoutTheirSectionAreStrayAndOpenSectionsAreCounted() {
        val (nesting, openSections) = BlockNesting.of(listOf(BlockDefinition.End, BlockDefinition.Else, repeat, BlockDefinition.Else, ifBlock, BlockDefinition.Else, BlockDefinition.Else, stop))
        assertEquals(listOf(Role.STRAY, Role.STRAY, Role.OPENS, Role.STRAY, Role.OPENS, Role.ELSE, Role.STRAY, Role.PLAIN), nesting.map { it.role })
        assertEquals(listOf(0, 0, 0, 1, 1, 1, 2, 2), nesting.map { it.depth })
        assertEquals(2, openSections)
    }

    @Test
    fun anIfOrRepeatBlockMovesBetweenEventsWithItsSectionAndAnElseOrEndBlockDoesNot() {
        val blocks = listOf(ifBlock, repeat, stop, BlockDefinition.End, BlockDefinition.Else, stop, BlockDefinition.End, stop, repeat, stop)
        assertEquals(MovingBlocks(0..6, isUnendedSection = false), BlockNesting.movingBetweenEvents(blocks, 0))
        assertEquals(MovingBlocks(1..3, isUnendedSection = false), BlockNesting.movingBetweenEvents(blocks, 1))
        assertEquals(MovingBlocks(2..2, isUnendedSection = false), BlockNesting.movingBetweenEvents(blocks, 2))
        assertEquals(null, BlockNesting.movingBetweenEvents(blocks, 3))
        assertEquals(null, BlockNesting.movingBetweenEvents(blocks, 4))
        assertEquals(MovingBlocks(8..9, isUnendedSection = true), BlockNesting.movingBetweenEvents(blocks, 8))
        assertEquals(6, BlockNesting.endOf(blocks, 0))
        assertEquals(null, BlockNesting.endOf(blocks, 8))
        assertEquals(null, BlockNesting.movingBetweenEvents(listOf(stop, BlockDefinition.End), 1))
    }
}
