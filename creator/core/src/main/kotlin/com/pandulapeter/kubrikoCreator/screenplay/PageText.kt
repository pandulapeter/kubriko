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

/**
 * Reads the shape of a page's Markdown: a `# Title`, a one-sentence summary as its first paragraph, sections under `##`
 * headings, and an optional `## Planned` list at the end. Only checked, never forced, so every reading tolerates a page
 * of any shape.
 */
public object PageText {

    /**
     * The heading of the section holding what is planned but not built yet.
     */
    public const val PLANNED: String = "Planned"

    /**
     * A section of a page: its heading line (without the `#` marks), how many marks it has, and the lines it spans, the
     * heading's included, up to the next heading of the same or a higher level.
     */
    public data class Section(
        val heading: String,
        val level: Int,
        val lines: IntRange,
    )

    /**
     * The page's `# ` heading, or null if it has none.
     */
    public fun titleOf(text: String): String? = headings(text.lines()).firstOrNull { it.second == 1 }?.third

    /**
     * The first paragraph after the title (or at the top, without one), joined into one line, or null if there is none.
     */
    public fun summaryOf(text: String): String? {
        val lines = text.lines()
        var index = lines.indexOfFirst { it.isNotBlank() }.takeIf { it >= 0 } ?: return null
        if (headingOf(lines[index])?.first == 1) index++
        while (index < lines.size && lines[index].isBlank()) index++
        if (index >= lines.size || headingOf(lines[index]) != null) return null
        val paragraph = lines.drop(index).takeWhile { it.isNotBlank() && headingOf(it) == null }
        return paragraph.joinToString(" ") { it.trim() }.ifEmpty { null }
    }

    /**
     * Every section of [text] with a heading of level 2 or deeper, in order.
     */
    public fun sectionsOf(text: String): List<Section> {
        val lines = text.lines()
        val headings = headings(lines).filter { it.second >= 2 }
        return headings.map { (index, level, heading) ->
            val end = headings.firstOrNull { it.first > index && it.second <= level }?.first ?: lines.size
            Section(heading, level, index until end)
        }
    }

    /**
     * The page's `## Planned` section, the first if it has several, or null if it has none.
     */
    public fun plannedSectionOf(text: String): Section? = sectionsOf(text).firstOrNull { it.level == 2 && it.heading.equals(PLANNED, ignoreCase = true) }

    /**
     * [text] with the first section headed [heading] (ignoring case) replaced by [replacement], which brings its own
     * heading, or null if there is no such section. The line breaks of [text] are kept.
     */
    public fun withSectionReplaced(text: String, heading: String, replacement: String): String? {
        val section = sectionsOf(text).firstOrNull { it.heading.equals(heading.trim(), ignoreCase = true) } ?: return null
        val separator = if ("\r\n" in text) "\r\n" else "\n"
        val lines = text.lines()
        val replaced = replacement.replace("\r\n", "\n").trimEnd('\n').split('\n')
        val after = lines.drop(section.lines.last + 1)
        val joined = lines.take(section.lines.first) + replaced + (if (after.isNotEmpty() && after.first().isNotBlank()) listOf("") else emptyList()) + after
        val result = joined.joinToString(separator)
        return if (text.endsWith('\n') && !result.endsWith('\n')) result + separator else result
    }

    /**
     * Each heading line of [lines]: its index, level, and text, outside fenced code blocks.
     */
    private fun headings(lines: List<String>): List<Triple<Int, Int, String>> {
        var isInCode = false
        return lines.mapIndexedNotNull { index, line ->
            if (line.trimStart().startsWith("```")) isInCode = !isInCode
            if (isInCode) null else headingOf(line)?.let { (level, heading) -> Triple(index, level, heading) }
        }
    }

    private fun headingOf(line: String): Pair<Int, String>? {
        val match = heading.matchEntire(line) ?: return null
        return match.groupValues[1].length to match.groupValues[2].trim().trimEnd('#').trim()
    }

    private val heading = Regex("(#{1,6})\\s+(.*)")
}
