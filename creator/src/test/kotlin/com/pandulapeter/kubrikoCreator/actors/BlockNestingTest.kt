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
}
