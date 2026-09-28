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

import com.pandulapeter.kubrikoShowcase.BuildConfig
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import com.pandulapeter.kubrikoShowcase.implementation.isAvailable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MenuItemIndexTest {

    @Test
    fun welcomeRowIsTheFirstRow() = assertEquals(0, ShowcaseEntry.entries.menuItemIndex(null))

    @Test
    fun unavailableEntriesMapToTheFirstRow() = ShowcaseEntry.entries
        .filterNot { it.isAvailable }
        .forEach { assertEquals(0, ShowcaseEntry.entries.menuItemIndex(it), it.name) }

    @Test
    fun availableEntriesFollowTheFirstCategoryLabelInOrder() {
        val indices = ShowcaseEntry.entries.filter { it.isAvailable }.map { ShowcaseEntry.entries.menuItemIndex(it) }
        assertEquals(2, indices.first())
        indices.zipWithNext().forEach { (previous, next) -> assertTrue(next > previous, "$previous is not followed by a larger index than $next") }
    }

    @Test
    fun entriesMapToTheirRowsWithEveryEntryShown() {
        if (!BuildConfig.ARE_TEST_EXAMPLES_ENABLED || !BuildConfig.SHOULD_SHOW_UNFINISHED_GAMES) return
        mapOf(
            ShowcaseEntry.WALLBREAKER to 2,
            ShowcaseEntry.BLOCKYS_JOURNEY to 5,
            ShowcaseEntry.CONTENT_SHADERS to 7,
            ShowcaseEntry.SHADER_ANIMATIONS to 12,
            ShowcaseEntry.AUDIO to 14,
            ShowcaseEntry.INPUT to 16,
            ShowcaseEntry.LICENSES to 18,
            ShowcaseEntry.ABOUT to 19,
        ).forEach { (entry, index) -> assertEquals(index, ShowcaseEntry.entries.menuItemIndex(entry), entry.name) }
    }
}
