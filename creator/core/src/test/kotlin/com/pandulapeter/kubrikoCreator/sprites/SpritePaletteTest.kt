/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sprites

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.project.OwnWrites
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(EditorMutation::class)
class SpritePaletteTest {
    private val root = testDirectory()
    private val palette = SpritePalette(root, OwnWrites())
    private val file = File(root, SpritePalette.FILE)

    @Test
    fun colorsAreKeptAsHexCodesAndNoneLeavesNoFile() {
        palette.set(listOf(0xFFFF0000, 0x80112233))
        assertEquals(listOf(0xFFFF0000, 0x80112233), palette.colors.value)
        assertEquals(listOf(0xFFFF0000, 0x80112233), SpritePalette(root, OwnWrites()).also { it.refresh() }.colors.value)
        assertEquals(true, "#FF0000" in file.readText() && "#80112233" in file.readText())
        palette.set(emptyList())
        assertFalse(file.exists())
        assertEquals(emptyList(), palette.colors.value)
    }

    @Test
    fun aPaletteThatCantBeReadHoldsNoColors() {
        file.apply { parentFile.mkdirs() }.writeText("not json")
        palette.refresh()
        assertEquals(emptyList(), palette.colors.value)
    }
}
