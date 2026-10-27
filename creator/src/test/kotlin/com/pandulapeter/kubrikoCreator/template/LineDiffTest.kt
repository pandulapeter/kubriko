/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import com.pandulapeter.kubrikoCreator.template.LineDiff.Kind
import com.pandulapeter.kubrikoCreator.template.LineDiff.Line
import kotlin.test.Test
import kotlin.test.assertEquals

class LineDiffTest {

    @Test
    fun linesAreKeptRemovedOrAdded() {
        assertEquals(
            listOf(Line(Kind.SAME, "a"), Line(Kind.REMOVED, "b"), Line(Kind.ADDED, "x"), Line(Kind.ADDED, "y"), Line(Kind.SAME, "c")),
            LineDiff.of("a\nb\nc", "a\nx\ny\nc"),
        )
        assertEquals(listOf(Line(Kind.SAME, "a")), LineDiff.of("a", "a"))
    }

    @Test
    fun unchangedLinesFarFromAChangeAreSkipped() {
        val lines = LineDiff.of((1..10).joinToString("\n"), (1..10).joinToString("\n").replace("\n6\n", "\n6\nnew\n"))
        assertEquals(
            listOf(null, Line(Kind.SAME, "5"), Line(Kind.SAME, "6"), Line(Kind.ADDED, "new"), Line(Kind.SAME, "7"), Line(Kind.SAME, "8"), null),
            LineDiff.withContext(lines),
        )
    }
}
