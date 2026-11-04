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

class PageChunksTest {
    private val page = "# The hero\n\nA knight\nwho jumps.\n\n```\nval x = 1\n\nval y = 2\n```\n\n- Runs\n- Jumps\n"

    @Test
    fun chunksAreRunsOfLinesWithCodeKeptWhole() = assertEquals(
        listOf(
            PageChunks.Chunk(0..0, "# The hero"),
            PageChunks.Chunk(2..3, "A knight\nwho jumps."),
            PageChunks.Chunk(5..9, "```\nval x = 1\n\nval y = 2\n```"),
            PageChunks.Chunk(11..12, "- Runs\n- Jumps"),
        ),
        PageChunks.of(page),
    )

    @Test
    fun aChunkEditedWithBlankLinesGoesBackToItsPlace() {
        val chunk = PageChunks.of(page)[1]
        val frame = PageChunks.frameOf(page, chunk)
        assertEquals(page, frame.compose(chunk.text))
        assertEquals(page.replace("A knight\nwho jumps.", "A knight.\n\nWho jumps."), frame.compose("A knight.\n\nWho jumps."))
    }

    @Test
    fun theFirstAndLastChunksAndWindowsLineBreaksAreFramed() {
        val text = "# A\r\n\r\nB"
        val chunks = PageChunks.of(text)
        assertEquals("# New\n\nB", PageChunks.frameOf(text, chunks[0]).compose("# New"))
        assertEquals("# A\n\nC", PageChunks.frameOf(text, chunks[1]).compose("C"))
    }

    @Test
    fun aNewChunkGoesAfterABlankLine() {
        assertEquals("# A\n\nNew\n", PageChunks.appendingTo("# A\n\n\n").compose("New"))
        assertEquals("New\n", PageChunks.appendingTo("").compose("New"))
    }
}
