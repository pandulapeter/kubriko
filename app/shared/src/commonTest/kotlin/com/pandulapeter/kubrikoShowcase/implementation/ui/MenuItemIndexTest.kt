/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui

import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import com.pandulapeter.kubrikoShowcase.implementation.isAvailable
import kotlin.test.Test
import kotlin.test.assertEquals

class MenuItemIndexTest {

    @Test
    fun welcomeRowIsTheFirstRow() = assertEquals(0, ShowcaseEntry.entries.menuItemIndex(null))

    @Test
    fun unavailableEntriesMapToTheFirstRow() = ShowcaseEntry.entries
        .filterNot { it.isAvailable }
        .forEach { assertEquals(0, ShowcaseEntry.entries.menuItemIndex(it), it.name) }

    @Test
    fun theFirstAvailableEntryFollowsTheWelcomeRowAndItsCategoryLabel() {
        val firstAvailableEntry = ShowcaseEntry.entries.first { it.isAvailable }

        assertEquals(2, ShowcaseEntry.entries.menuItemIndex(firstAvailableEntry))
    }

    @Test
    fun entriesOfOneCategoryAreOnConsecutiveRowsAndEachNewCategoryAddsALabelRow() = ShowcaseEntry.entries
        .filter { it.isAvailable }
        .groupBy { it.type }
        .values
        .flatten()
        .zipWithNext()
        .forEach { (previous, next) ->
            val expectedGap = if (previous.type == next.type) 1 else 2
            assertEquals(
                expectedGap,
                ShowcaseEntry.entries.menuItemIndex(next) - ShowcaseEntry.entries.menuItemIndex(previous),
                "${previous.name} -> ${next.name}",
            )
        }
}
