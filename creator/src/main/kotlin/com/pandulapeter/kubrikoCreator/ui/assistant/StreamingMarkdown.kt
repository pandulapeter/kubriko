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

/**
 * Reads a message that keeps growing, as an agent streams it, with [MarkdownParser], parsing only what follows the last
 * break that can't change meaning again: a blank line outside code, followed by a line that doesn't start with
 * whitespace (which could otherwise continue a list item). Each call gives what [MarkdownParser.parse] would.
 */
internal class StreamingMarkdown {
    private var stableText = ""
    private var stableBlocks = emptyList<MarkdownBlock>()

    fun parse(text: String): List<MarkdownBlock> {
        if (!text.startsWith(stableText)) {
            stableText = ""
            stableBlocks = emptyList()
        }
        val boundary = lastBoundary(text, from = stableText.length)
        if (boundary > stableText.length) {
            stableBlocks = stableBlocks + MarkdownParser.parse(text.substring(stableText.length, boundary))
            stableText = text.substring(0, boundary)
        }
        return if (boundary < text.length) stableBlocks + MarkdownParser.parse(text.substring(boundary)) else stableBlocks
    }

    /**
     * The end of the last break in [text] after [from], which is itself a break (or the start), or [from] if there is none.
     */
    private fun lastBoundary(text: String, from: Int): Int {
        var boundary = from
        var isInCode = false
        var isPreviousLineBlank = false
        var lineStart = from
        while (lineStart < text.length) {
            val lineEnd = text.indexOf('\n', lineStart).let { if (it < 0) text.length else it }
            if (lineEnd == text.length) break
            val line = text.substring(lineStart, lineEnd)
            val isBlank = line.isBlank()
            if (!isInCode && isPreviousLineBlank && !isBlank && !line[0].isWhitespace()) boundary = lineStart
            if (line.trim().startsWith(FENCE)) isInCode = !isInCode
            isPreviousLineBlank = isBlank && !isInCode
            lineStart = lineEnd + 1
        }
        return boundary
    }

    private companion object {
        const val FENCE = "```"
    }
}
