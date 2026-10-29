/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.foundation.lazy.LazyListItemInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class EventBlocksTest {

    private fun item(key: Any, offset: Int, size: Int) = object : LazyListItemInfo {
        override val index = 0
        override val key = key
        override val offset = offset
        override val size = size
    }

    @Test
    fun laidOutBlockRowsAreInRootCoordinatesFromTheFirstLaidOutIndex() {
        val rows = laidOutRows(listOf(item(3, -10, 40), item(4, 34, 40), item(BLOCK_LIST_END_KEY, 78, 3)), itemsTop = 100f, blockCount = 5)
        assertEquals(LaidOutRows(3, listOf(90f..130f, 134f..174f)), rows)
    }

    @Test
    fun withoutLaidOutRowsTheGapIsAtTheEndOnlyIfTheListShowsIt() {
        assertEquals(LaidOutRows(5, emptyList()), laidOutRows(listOf(item(BLOCK_LIST_END_KEY, 0, 3)), itemsTop = 0f, blockCount = 5))
        assertEquals(LaidOutRows.NONE, laidOutRows(listOf(item("empty", 0, 20), item(BLOCK_LIST_END_KEY, 24, 3)), itemsTop = 0f, blockCount = 0))
        assertEquals(LaidOutRows.NONE, laidOutRows(emptyList(), itemsTop = 0f, blockCount = 5))
    }
}
