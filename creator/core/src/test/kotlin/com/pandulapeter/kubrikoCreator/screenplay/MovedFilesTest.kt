/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.screenplay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MovedFilesTest {

    @Test
    fun `a file is followed through every move, and a move back ends the chain`() {
        val moved = MovedFiles()
        assertNull(moved.whereIs("a.md"))
        moved.record("a.md", "b.md")
        moved.record("b.md", "c.md")
        assertEquals("c.md", moved.whereIs("a.md"))
        moved.record("c.md", "a.md")
        assertEquals("a.md", moved.whereIs("c.md"))
        assertEquals("a.md", moved.whereIs("b.md"))
        assertNull(moved.whereIs("a.md"))
    }

    @Test
    fun `only the latest moves are kept`() {
        val moved = MovedFiles()
        repeat(300) { moved.record("$it.md", "${it + 1000}.md") }
        assertNull(moved.whereIs("0.md"))
        assertEquals("1299.md", moved.whereIs("299.md"))
    }
}
