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
 * A line of Markdown with its markers removed, and the ranges of [text] they styled.
 */
internal data class InlineMarkdown(
    val text: String,
    val spans: List<Span> = emptyList(),
) {
    data class Span(
        val style: Style,
        val start: Int,
        val end: Int,
        val url: String? = null,
    )

    enum class Style {
        BOLD,
        ITALIC,
        CODE,
        LINK,
    }

    companion object {

        /**
         * Reads `**bold**`, `*italic*` (or with underscores), `` `code` ``, and `[links](url)`. A marker without its
         * closing one, as while a message streams in, stays as it is.
         */
        fun parse(source: String): InlineMarkdown {
            val text = StringBuilder()
            val spans = mutableListOf<Span>()
            parseInto(source, text, spans)
            return InlineMarkdown(text.toString(), spans.sortedBy { it.start })
        }

        private fun parseInto(source: String, text: StringBuilder, spans: MutableList<Span>) {
            var index = 0
            while (index < source.length) {
                val character = source[index]
                val next = when {
                    character == '\\' && index + 1 < source.length && source[index + 1] in escapable -> {
                        text.append(source[index + 1])
                        index + 2
                    }
                    character == '`' -> code(source, index, text, spans)
                    source.startsWith("**", index) || source.startsWith("__", index) -> emphasis(source, index, 2, Style.BOLD, text, spans)
                    (character == '*' || character == '_') && isEmphasisStart(source, index) -> emphasis(source, index, 1, Style.ITALIC, text, spans)
                    character == '[' -> link(source, index, text, spans)
                    else -> null
                }
                if (next == null) {
                    text.append(character)
                    index++
                } else {
                    index = next
                }
            }
        }

        private fun code(source: String, start: Int, text: StringBuilder, spans: MutableList<Span>): Int? {
            val end = source.indexOf('`', start + 1).takeIf { it > start + 1 } ?: return null
            spans += Span(Style.CODE, text.length, text.length + end - start - 1)
            text.append(source, start + 1, end)
            return end + 1
        }

        private fun emphasis(source: String, start: Int, length: Int, style: Style, text: StringBuilder, spans: MutableList<Span>): Int? {
            val marker = source.substring(start, start + length)
            val end = source.indexOf(marker, start + length).takeIf { it > start + length } ?: return null
            if (length == 1 && source.startsWith(marker + marker, end)) return null
            val spanStart = text.length
            parseInto(source.substring(start + length, end), text, spans)
            spans += Span(style, spanStart, text.length)
            return end + length
        }

        private fun link(source: String, start: Int, text: StringBuilder, spans: MutableList<Span>): Int? {
            val middle = source.indexOf("](", start + 1).takeIf { it > start + 1 } ?: return null
            if ('\n' in source.substring(start, middle)) return null
            val end = source.indexOf(')', middle + 2).takeIf { it > middle + 2 } ?: return null
            val spanStart = text.length
            parseInto(source.substring(start + 1, middle), text, spans)
            spans += Span(Style.LINK, spanStart, text.length, source.substring(middle + 2, end))
            return end + 1
        }

        /**
         * Whether a single `*` or `_` at [index] opens emphasis: followed by something other than a space, and for `_`
         * not inside a word (`snake_case_names` stay as they are).
         */
        private fun isEmphasisStart(source: String, index: Int): Boolean {
            val following = source.getOrNull(index + 1) ?: return false
            if (following.isWhitespace()) return false
            return source[index] == '*' || source.getOrNull(index - 1)?.isLetterOrDigit() != true
        }

        private val escapable = setOf('\\', '`', '*', '_', '[', ']', '#')
    }
}
