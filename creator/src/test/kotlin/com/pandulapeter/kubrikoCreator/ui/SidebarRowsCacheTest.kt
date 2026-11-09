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
import kotlin.test.assertSame

class SidebarRowsCacheTest {

    @Test
    fun aValueIsMadeAgainOnlyWhenItsInputsChange() {
        val cache = SidebarRowsCache()
        var made = 0
        val items = listOf("Ball" to "", "Bat" to "enemies")
        val first = cache.valueOf("actors", items, setOf("enemies")) { made++; folderTreeRows(items, emptySet(), setOf("enemies")) }
        assertSame(first, cache.valueOf("actors", items.toList(), setOf("enemies")) { made++; emptyList<FolderTreeRow>() })
        assertEquals(1, made)
        cache.valueOf("actors", items, emptySet<String>()) { made++; emptyList<FolderTreeRow>() }
        assertEquals(2, made)
        cache.valueOf("scenes", items, emptySet<String>()) { made++; emptyList<FolderTreeRow>() }
        assertEquals(3, made, "each key has a value of its own")
    }

    @Test
    fun onlyTheRowThatChangedLightsUp() {
        val flash = SidebarFlash(name = "Ball", key = 3)
        assertEquals(3, flash.keyFor("Ball"))
        assertEquals(null, flash.keyFor("Bat"))
        assertEquals(null, SidebarFlash().keyFor("Ball"))
    }
}
