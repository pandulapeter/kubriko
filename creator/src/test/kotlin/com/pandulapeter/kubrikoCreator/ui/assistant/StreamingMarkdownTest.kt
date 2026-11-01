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

class StreamingMarkdownTest {
    private val message = """
        # The plan

        First I'll read **Ball**, then:

        - add a `Set velocity` Block
          that goes on here

          and here, after a blank line
        - change the speed

        1. one
        2. two
        > a quote
        > that goes on

        ```kotlin
        val x = 1

        val y = 2
        ```

        ---
        Done.
        ```
        an unclosed fence

        with a blank line
    """.trimIndent()

    @Test
    fun everyPrefixReadsAsAWholeParseWould() {
        val streaming = StreamingMarkdown()
        (0..message.length).forEach { end ->
            val text = message.substring(0, end)
            assertEquals(MarkdownParser.parse(text), streaming.parse(text), "After ${text.length} characters")
        }
    }

    @Test
    fun chunksOfAnySizeReadAsAWholeParseWould() = listOf(3, 17, 64).forEach { size ->
        val streaming = StreamingMarkdown()
        (size..message.length + size step size).forEach { end ->
            val text = message.substring(0, end.coerceAtMost(message.length))
            assertEquals(MarkdownParser.parse(text), streaming.parse(text), "Chunks of $size, after ${text.length} characters")
        }
    }

    @Test
    fun aTextThatDoesntContinueThePreviousOneIsReadAfresh() {
        val streaming = StreamingMarkdown()
        streaming.parse(message)
        assertEquals(MarkdownParser.parse("Something else\n\nentirely"), streaming.parse("Something else\n\nentirely"))
    }
}
