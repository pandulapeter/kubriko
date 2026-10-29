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

import com.pandulapeter.kubrikoCreator.ui.assistant.InlineMarkdown.Span
import com.pandulapeter.kubrikoCreator.ui.assistant.InlineMarkdown.Style
import kotlin.test.Test
import kotlin.test.assertEquals

class InlineMarkdownTest {

    @Test
    fun markersBecomeStyledRanges() {
        assertEquals(
            InlineMarkdown(
                text = "Set speed to 400 in Ball, see docs.",
                spans = listOf(
                    Span(Style.BOLD, 0, 9),
                    Span(Style.CODE, 13, 16),
                    Span(Style.ITALIC, 20, 24),
                    Span(Style.LINK, 30, 34, "https://kubriko.dev"),
                ),
            ),
            InlineMarkdown.parse("**Set speed** to `400` in *Ball*, see [docs](https://kubriko.dev)."),
        )
    }

    @Test
    fun stylesNest() {
        assertEquals(
            InlineMarkdown("a bold code", listOf(Span(Style.BOLD, 2, 11), Span(Style.CODE, 7, 11))),
            InlineMarkdown.parse("a **bold `code`**"),
        )
    }

    @Test
    fun unclosedAndInWordMarkersStayAsTheyAre() {
        assertEquals(InlineMarkdown("snake_case_name and **open"), InlineMarkdown.parse("snake_case_name and **open"))
        assertEquals(InlineMarkdown("2 * 3 * 4"), InlineMarkdown.parse("2 * 3 * 4"))
        assertEquals(InlineMarkdown("`"), InlineMarkdown.parse("`"))
        assertEquals(InlineMarkdown("[not a link] (x)"), InlineMarkdown.parse("[not a link] (x)"))
    }

    @Test
    fun escapedMarkersAreText() {
        assertEquals(InlineMarkdown("*not italic*"), InlineMarkdown.parse("\\*not italic\\*"))
    }
}
