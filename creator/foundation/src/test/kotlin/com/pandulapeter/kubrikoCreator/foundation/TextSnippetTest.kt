/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.foundation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TextSnippetTest {

    @Test
    fun aShortLineIsTrimmedWithEveryMatchMarked() = assertEquals(
        TextSnippet("wall and WALL", listOf(0..3, 9..12)),
        TextSnippet.of("   wall and WALL  ", "wall"),
    )

    @Test
    fun aLongLineIsCutAroundItsFirstMatch() {
        val snippet = TextSnippet.of("x".repeat(200) + "needle" + "y".repeat(200), "needle", length = 30)!!
        assertEquals("…" + "x".repeat(10) + "needle" + "y".repeat(14) + "…", snippet.text)
        assertEquals("needle", snippet.text.substring(snippet.ranges.single().first, snippet.ranges.single().last + 1))
    }

    @Test
    fun aLineWithoutTheQueryOrAnEmptyQueryHasNoSnippet() {
        assertNull(TextSnippet.of("wall", "door"))
        assertNull(TextSnippet.of("wall", ""))
    }
}
