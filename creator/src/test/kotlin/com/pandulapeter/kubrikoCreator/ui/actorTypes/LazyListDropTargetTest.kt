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

class LazyListDropTargetTest {

    private fun item(index: Int, offset: Int, size: Int, contentType: Any? = ROW) = object : LazyListItemInfo {
        override val index = index
        override val key: Any = index
        override val offset = offset
        override val size = size
        override val contentType = contentType
    }

    @Test
    fun laidOutRowsAreInRootCoordinatesFromTheFirstLaidOutIndex() {
        val rows = laidOutRows(listOf(item(3, -10, 40), item(4, 34, 40), item(5, 78, 3, contentType = null)), itemsTop = 100f, rowCount = 5, rowContentType = ROW)
        assertEquals(LaidOutRows(3, listOf(90f..130f, 134f..174f)), rows)
    }

    @Test
    fun withoutLaidOutRowsTheGapIsAtTheEndOnlyIfTheListShowsWhatComesAfterThem() {
        assertEquals(LaidOutRows(5, emptyList()), laidOutRows(listOf(item(5, 0, 3, contentType = null)), itemsTop = 0f, rowCount = 5, rowContentType = ROW))
        assertEquals(LaidOutRows.NONE, laidOutRows(listOf(item(0, 0, 20, contentType = null), item(1, 24, 3, contentType = null)), itemsTop = 0f, rowCount = 0, rowContentType = ROW))
        assertEquals(LaidOutRows.NONE, laidOutRows(emptyList(), itemsTop = 0f, rowCount = 5, rowContentType = ROW))
    }

    private companion object {
        const val ROW = "row"
    }
}
