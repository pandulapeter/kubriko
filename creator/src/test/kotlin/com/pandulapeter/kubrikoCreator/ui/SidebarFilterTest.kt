/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SidebarFilterTest {

    @Test
    fun aNameMatchesWhatItContainsIgnoringCaseAndSurroundingSpaces() {
        assertTrue(matchesSidebarFilter("enemies/Bat", "bat"))
        assertTrue(matchesSidebarFilter("Ball", " BA "))
        assertFalse(matchesSidebarFilter("Ball", "x"))
        assertTrue(matchesSidebarFilter("Ball", ""))
    }

    @Test
    fun aNameMatchesItsLettersInOrderButAPathOnlyWhatItContains() {
        assertTrue(matchesSidebarFilter("Brick", "brk"))
        assertTrue(matchesSidebarFilter("walls/Brick", "brk"))
        assertTrue(matchesSidebarFilter("levels/cave", "ls/ca"))
        assertFalse(matchesSidebarFilter("levels/cave", "lc"), "the letters of a path's folders aren't taken one by one")
    }

    @Test
    fun filteringKeepsOnlyTheMatchingItemsAndTheFoldersHoldingThem() {
        val state = FolderTreeGroupState(
            items = listOf("Bat" to "enemies/flying", "Ball" to "", "Brick" to "walls"),
            folders = setOf("enemies", "enemies/flying", "walls", "empty"),
            selectedName = "Ball",
        )
        assertEquals(
            FolderTreeGroupState(
                items = listOf("Bat" to "enemies/flying"),
                folders = setOf("enemies", "enemies/flying"),
                selectedName = "Ball",
            ),
            state.filtered("bat"),
        )
    }
}
