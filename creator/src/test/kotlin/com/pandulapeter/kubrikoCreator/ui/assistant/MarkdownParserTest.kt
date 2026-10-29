/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.assistant

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownParserTest {

    @Test
    fun blocksAreReadAsAgentsWriteThem() {
        val text = """
            ## What I changed

            The Ball now moves
            twice as fast.

            - Ball: speed 400
              (was 200)
              - nested
            1. First
            2) Second
            > A note
            > on two lines
            ---
            ```kotlin
            val x = 1

            val y = 2
            ```
        """.trimIndent()
        assertEquals(
            listOf(
                MarkdownBlock.Heading(2, "What I changed"),
                MarkdownBlock.Paragraph("The Ball now moves\ntwice as fast."),
                MarkdownBlock.ListItem("Ball: speed 400 (was 200)"),
                MarkdownBlock.ListItem("nested", depth = 1),
                MarkdownBlock.ListItem("First", number = "1"),
                MarkdownBlock.ListItem("Second", number = "2"),
                MarkdownBlock.Quote("A note\non two lines"),
                MarkdownBlock.Rule,
                MarkdownBlock.Code("val x = 1\n\nval y = 2"),
            ),
            MarkdownParser.parse(text),
        )
    }

    @Test
    fun aFenceStillStreamingInHoldsTheRest() {
        assertEquals(
            listOf(MarkdownBlock.Paragraph("Here:"), MarkdownBlock.Code("fun a()\n# not a heading")),
            MarkdownParser.parse("Here:\n```\nfun a()\n# not a heading"),
        )
    }

    @Test
    fun emptyTextHasNoBlocks() {
        assertEquals(emptyList(), MarkdownParser.parse(""))
        assertEquals(emptyList(), MarkdownParser.parse("\n\n"))
    }
}
