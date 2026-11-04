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

import com.pandulapeter.kubrikoCreator.project.withUnixLineBreaks

/**
 * A page split into the chunks the page editor edits one at a time: runs of lines between blank ones, a fenced code block
 * kept whole with its blank lines. Line breaks are read as `\n`; the page keeps its own when it is written.
 */
public object PageChunks {

    /**
     * One chunk: its [text] and the [lines] of the page it spans.
     */
    public data class Chunk(
        val lines: IntRange,
        val text: String,
    )

    /**
     * The text around a chunk being edited, so that what is typed into it, blank lines included, goes back to its place
     * without the chunks around it moving while it is edited.
     */
    public data class Frame(
        val before: String,
        val after: String,
    ) {
        public fun compose(draft: String): String = before + draft + after
    }

    public fun of(text: String): List<Chunk> {
        val lines = text.withUnixLineBreaks().split('\n')
        val chunks = ArrayList<Chunk>()
        var start = -1
        var isInCode = false
        lines.forEachIndexed { index, line ->
            if (line.trimStart().startsWith(FENCE)) isInCode = !isInCode
            when {
                line.isNotBlank() || isInCode -> if (start < 0) start = index
                start >= 0 -> {
                    chunks += Chunk(start until index, lines.subList(start, index).joinToString("\n"))
                    start = -1
                }
            }
        }
        if (start >= 0) chunks += Chunk(start until lines.size, lines.subList(start, lines.size).joinToString("\n").trimEnd('\n'))
        return chunks
    }

    /**
     * The text around [chunk] of [text].
     */
    public fun frameOf(text: String, chunk: Chunk): Frame {
        val lines = text.withUnixLineBreaks().split('\n')
        val before = lines.take(chunk.lines.first).joinToString("\n") + if (chunk.lines.first > 0) "\n" else ""
        val rest = lines.drop(chunk.lines.last + 1)
        return Frame(before, if (rest.isEmpty()) "" else "\n" + rest.joinToString("\n"))
    }

    /**
     * The frame of a new chunk at the end of [text], after a blank line.
     */
    public fun appendingTo(text: String): Frame {
        val base = text.withUnixLineBreaks().trimEnd('\n', ' ')
        return Frame(if (base.isEmpty()) "" else base + "\n\n", "\n")
    }

    private const val FENCE = "```"
}
