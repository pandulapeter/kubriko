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
 * Reads the Markdown agents write (headings, lists, quotes, fenced code, rules, and paragraphs) into blocks. A code
 * fence that isn't closed yet, as while a message streams in, holds the rest of the text.
 */
internal object MarkdownParser {
    private val heading = Regex("^(#{1,6})\\s+(.*)$")
    private val bullet = Regex("^(\\s*)[-*+]\\s+(.*)$")
    private val numbered = Regex("^(\\s*)(\\d{1,9})[.)]\\s+(.*)$")
    private val rule = Regex("^\\s{0,3}([-*_])(\\s*\\1){2,}\\s*$")

    fun parse(text: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val paragraph = mutableListOf<String>()
        val quote = mutableListOf<String>()
        fun flush() {
            if (paragraph.isNotEmpty()) blocks += MarkdownBlock.Paragraph(paragraph.joinToString("\n"))
            if (quote.isNotEmpty()) blocks += MarkdownBlock.Quote(quote.joinToString("\n"))
            paragraph.clear()
            quote.clear()
        }
        val lines = text.lines()
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            val trimmed = line.trim()
            index++
            if (trimmed.startsWith(FENCE)) {
                flush()
                val code = mutableListOf<String>()
                while (index < lines.size && !lines[index].trim().startsWith(FENCE)) code += lines[index++]
                index++
                blocks += MarkdownBlock.Code(code.joinToString("\n"))
                continue
            }
            if (trimmed.isEmpty()) {
                flush()
                continue
            }
            if (trimmed.startsWith(">")) {
                if (paragraph.isNotEmpty()) flush()
                quote += trimmed.removePrefix(">").trimStart()
                continue
            }
            val block = rule.matchEntire(line)?.let { MarkdownBlock.Rule }
                ?: heading.matchEntire(trimmed)?.let { MarkdownBlock.Heading(it.groupValues[1].length, it.groupValues[2].trimEnd('#', ' ')) }
                ?: bullet.matchEntire(line)?.let { MarkdownBlock.ListItem(it.groupValues[2], depth = depthOf(it.groupValues[1])) }
                ?: numbered.matchEntire(line)?.let { MarkdownBlock.ListItem(it.groupValues[3], it.groupValues[2], depthOf(it.groupValues[1])) }
            if (block != null) {
                flush()
                blocks += block
            } else {
                val previous = blocks.lastOrNull()
                if (paragraph.isEmpty() && quote.isEmpty() && previous is MarkdownBlock.ListItem && line.startsWith(" ")) {
                    blocks[blocks.lastIndex] = previous.copy(text = previous.text + " " + trimmed)
                } else {
                    if (quote.isNotEmpty()) flush()
                    paragraph += trimmed
                }
            }
        }
        flush()
        return blocks
    }

    private fun depthOf(indentation: String) = indentation.replace("\t", "    ").length / 2

    private const val FENCE = "```"
}
