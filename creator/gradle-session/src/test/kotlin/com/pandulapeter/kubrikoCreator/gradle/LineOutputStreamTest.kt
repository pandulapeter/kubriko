/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.gradle

import kotlin.test.Test
import kotlin.test.assertEquals

class LineOutputStreamTest {

    @Test
    fun linesAreSplitWithoutTheirLineBreaks() {
        val lines = mutableListOf<String>()
        LineOutputStream(lines::add).write("first\nsecond\r\n\nunfinished".toByteArray())
        assertEquals(listOf("first", "second", ""), lines)
    }

    @Test
    fun multiByteCharactersSurviveBeingWrittenOneByteAtATime() {
        val lines = mutableListOf<String>()
        val stream = LineOutputStream(lines::add)
        "Péter — ✓\n".toByteArray().forEach { stream.write(it.toInt()) }
        assertEquals(listOf("Péter — ✓"), lines)
    }
}
