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

import java.io.ByteArrayOutputStream

/**
 * The links and images of a page's Markdown. Those pointing into the project are its mentions: relative links to the
 * thing's own file, so that they work on GitHub and in IntelliJ too. Links inside code are no links.
 */
public object PageLinks {

    /**
     * A link (`[label](destination)`) or image (`![label](destination)`) of a page.
     *
     * @param destination The destination as written, without the angle brackets it may be written in.
     * @param destinationRange Where [destination] is in the page's text.
     * @param isInAngleBrackets Whether the destination is written as `<destination>`, which needs no escaping.
     */
    public data class Link(
        val label: String,
        val destination: String,
        val destinationRange: IntRange,
        val isImage: Boolean,
        val isInAngleBrackets: Boolean,
    )

    /**
     * Every link and image of [text], in the order of their destinations, outside fenced code blocks and code spans.
     */
    public fun linksOf(text: String): List<Link> {
        val links = ArrayList<Link>()
        var offset = 0
        var fence: String? = null
        text.split('\n').forEach { line ->
            val trimmed = line.trimStart()
            val marker = fenceMarkerOf(trimmed)
            when {
                fence != null -> if (marker != null && marker.first() == fence.first() && marker.length >= fence.length) fence = null
                marker != null -> fence = marker
                else -> scanInline(line, 0, line.length, offset, links)
            }
            offset += line.length + 1
        }
        // A linked picture's image is found after the link around it, but comes first in the text.
        return links.sortedBy { it.destinationRange.first }
    }

    /**
     * The file of the project, as a path from its root, that [destination] names when written in the page at [pagePath]
     * (a path from the project's root too), or null if it names none: a web address, a path outside the project or from
     * the root of the disk, or only a place in the page itself (`#heading`).
     */
    public fun resolve(pagePath: String, destination: String): String? {
        val path = destination.take(suffixStartOf(destination))
        if (path.isEmpty() || path.startsWith('/') || path.startsWith('\\') || scheme.containsMatchIn(path)) return null
        val segments = ArrayList(folderSegmentsOf(pagePath))
        decode(path).split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> if (segments.isEmpty()) return null else segments.removeAt(segments.lastIndex)
                else -> segments += segment
            }
        }
        return segments.joinToString("/").ifEmpty { null }
    }

    /**
     * How the page at [pagePath] links to the project's file at [path], both paths from the project's root.
     */
    public fun destinationOf(pagePath: String, path: String): String = encode(relativePath(pagePath, path))

    /**
     * [text], the page at [pagePath], with every link and image that pointed at something [moves] moved pointing at
     * where it is now. If the page itself moved, its other links into the project are written again from where it is
     * now, so that they still point at the same files. Everything else is left as it was written.
     */
    public fun withMovesFollowed(text: String, pagePath: String, moves: List<PathMove>): String {
        val newPagePath = moves.applyTo(pagePath) ?: pagePath
        val result = StringBuilder(text)
        linksOf(text).asReversed().forEach { link ->
            val resolved = resolve(pagePath, link.destination) ?: return@forEach
            val moved = moves.applyTo(resolved)
            if (moved == null && newPagePath == pagePath) return@forEach
            val relative = relativePath(newPagePath, moved ?: resolved)
            val suffix = link.destination.substring(suffixStartOf(link.destination))
            val replacement = (if (link.isInAngleBrackets) relative else encode(relative)) + suffix
            result.replace(link.destinationRange.first, link.destinationRange.last + 1, replacement)
        }
        return result.toString()
    }

    private fun scanInline(line: String, start: Int, end: Int, offset: Int, links: MutableList<Link>) {
        var index = start
        while (index < end) {
            when (line[index]) {
                '\\' -> index += 2
                '`' -> index = afterCodeSpan(line, index, end)
                '[' -> {
                    val parsed = parseLink(line, index, end)
                    if (parsed == null) {
                        index++
                    } else {
                        val isImage = index > start && line[index - 1] == '!' && (index < start + 2 || line[index - 2] != '\\')
                        links += Link(
                            label = line.substring(index + 1, parsed.labelEnd),
                            destination = line.substring(parsed.destinationStart, parsed.destinationEnd),
                            destinationRange = (offset + parsed.destinationStart) until (offset + parsed.destinationEnd),
                            isImage = isImage,
                            isInAngleBrackets = parsed.isInAngleBrackets,
                        )
                        // A label may hold an image of its own, as a linked picture does.
                        scanInline(line, index + 1, parsed.labelEnd, offset, links)
                        index = parsed.end
                    }
                }
                else -> index++
            }
        }
    }

    private fun afterCodeSpan(line: String, start: Int, end: Int): Int {
        var runEnd = start
        while (runEnd < end && line[runEnd] == '`') runEnd++
        val run = runEnd - start
        var index = runEnd
        while (index < end) {
            if (line[index] == '`') {
                var closing = index
                while (closing < end && line[closing] == '`') closing++
                if (closing - index == run) return closing
                index = closing
            } else {
                index++
            }
        }
        return runEnd
    }

    private class ParsedLink(
        val labelEnd: Int,
        val destinationStart: Int,
        val destinationEnd: Int,
        val isInAngleBrackets: Boolean,
        val end: Int,
    )

    private fun parseLink(line: String, start: Int, end: Int): ParsedLink? {
        val labelEnd = labelEndOf(line, start, end) ?: return null
        if (labelEnd + 1 >= end || line[labelEnd + 1] != '(') return null
        var index = labelEnd + 2
        while (index < end && line[index] == ' ') index++
        val destinationStart: Int
        val destinationEnd: Int
        val isInAngleBrackets = index < end && line[index] == '<'
        if (isInAngleBrackets) {
            destinationStart = index + 1
            destinationEnd = line.indexOf('>', destinationStart).takeIf { it in destinationStart until end } ?: return null
            index = destinationEnd + 1
        } else {
            destinationStart = index
            var depth = 0
            while (index < end && !line[index].isWhitespace()) {
                val character = line[index]
                if (character == '\\') {
                    index += 2
                    continue
                }
                if (character == '(') depth++
                if (character == ')') if (depth == 0) break else depth--
                index++
            }
            destinationEnd = index.coerceAtMost(end)
            if (destinationEnd == destinationStart) return null
        }
        while (index < end && line[index] == ' ') index++
        if (index < end && line[index] in titleOpeners) {
            val closing = titleClosers.getValue(line[index])
            index = line.indexOf(closing, index + 1).takeIf { it in 0 until end }?.plus(1) ?: return null
            while (index < end && line[index] == ' ') index++
        }
        if (index >= end || line[index] != ')') return null
        return ParsedLink(labelEnd, destinationStart, destinationEnd, isInAngleBrackets, index + 1)
    }

    private fun labelEndOf(line: String, start: Int, end: Int): Int? {
        var depth = 0
        var index = start + 1
        while (index < end) {
            when (line[index]) {
                '\\' -> index++
                '`' -> {
                    index = afterCodeSpan(line, index, end)
                    continue
                }
                '[' -> depth++
                ']' -> if (depth == 0) return index else depth--
            }
            index++
        }
        return null
    }

    private fun fenceMarkerOf(trimmedLine: String): String? {
        val character = trimmedLine.firstOrNull()?.takeIf { it == '`' || it == '~' } ?: return null
        val marker = trimmedLine.takeWhile { it == character }
        return marker.takeIf { it.length >= 3 }
    }

    private fun relativePath(pagePath: String, path: String): String {
        val from = folderSegmentsOf(pagePath)
        val to = path.split('/')
        var shared = 0
        while (shared < from.size && shared < to.size - 1 && from[shared] == to[shared]) shared++
        return (List(from.size - shared) { ".." } + to.drop(shared)).joinToString("/")
    }

    private fun folderSegmentsOf(pagePath: String) = pagePath.split('/').dropLast(1).filter { it.isNotEmpty() }

    /**
     * Where a destination's place in its file (`#heading`) or query starts, or its length if it has neither.
     */
    private fun suffixStartOf(destination: String) = destination.indexOfFirst { it == '#' || it == '?' }.takeIf { it >= 0 } ?: destination.length

    private fun encode(path: String) = buildString {
        path.forEach { character ->
            if (character in escaped) append('%').append(character.code.toString(16).uppercase().padStart(2, '0')) else append(character)
        }
    }

    private fun decode(path: String): String {
        if ('%' !in path) return path
        val bytes = ByteArrayOutputStream()
        var index = 0
        while (index < path.length) {
            val code = if (path[index] == '%' && index + 2 < path.length) path.substring(index + 1, index + 3).toIntOrNull(16) else null
            if (code != null) {
                bytes.write(code)
                index += 3
            } else {
                bytes.write(path[index].toString().toByteArray(Charsets.UTF_8))
                index++
            }
        }
        return bytes.toString(Charsets.UTF_8)
    }

    private val scheme = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")
    private val escaped = setOf(' ', '%', '(', ')', '<', '>', '#', '?', '\\', '[', ']')
    private val titleOpeners = setOf('"', '\'', '(')
    private val titleClosers = mapOf('"' to '"', '\'' to '\'', '(' to ')')
}
