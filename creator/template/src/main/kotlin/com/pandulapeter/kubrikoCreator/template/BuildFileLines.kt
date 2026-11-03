/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import com.pandulapeter.kubrikoCreator.project.BuildScript

/**
 * Line edits of a template's build file that turning its targets on and off share.
 */
internal object BuildFileLines {
    private const val DESKTOP_TARGET = "jvm(\"desktop\")"

    /**
     * [text] with [block] (lines indented as one of the `kotlin` block's targets) right after the desktop target, or
     * null if the file doesn't declare it as the template does.
     */
    fun withTarget(text: String, block: List<String>): String? {
        val lines = text.lines()
        val desktop = lines.indices.singleOrNull { lines[it].trim() == DESKTOP_TARGET } ?: return null
        val indent = lines[desktop].takeWhile(Char::isWhitespace)
        return (lines.take(desktop + 1) + block.map { if (it.isEmpty()) it else indent + it } + lines.drop(desktop + 1)).joinToString("\n")
    }

    /**
     * [text] without the block opened on the one line [isStart] accepts by its index (and the [annotation] line right above
     * it, if it has one), [text] itself if no line opens one, or null if more than one does or it isn't closed.
     */
    fun withoutBlock(text: String, annotation: String? = null, isStart: (lines: List<String>, index: Int) -> Boolean): String? {
        val lines = text.lines()
        val starts = lines.indices.filter { isStart(lines, it) }
        val start = starts.singleOrNull() ?: return if (starts.isEmpty()) text else null
        val end = BuildScript.blockEnd(lines, start) ?: return null
        val first = if (annotation != null && start > 0 && lines[start - 1].trim() == annotation) start - 1 else start
        return (lines.take(first) + lines.drop(end + 1)).joinToString("\n")
    }

    /**
     * [text] with [block] at its end, after an empty line.
     */
    fun withAppended(text: String, block: String) = text.trimEnd() + "\n\n" + block

    /**
     * [text] without [block] where [withAppended] put it (or anywhere), [text] itself if it doesn't have it, or null if it has
     * a block that starts with the same line but was changed.
     */
    fun withoutAppended(text: String, block: String): String? {
        val trimmed = block.trimEnd()
        return when {
            "\n\n$trimmed" in text -> text.replace("\n\n$trimmed", "")
            trimmed in text -> text.replace(trimmed, "")
            block.lineSequence().first() in text -> null
            else -> text
        }
    }

    /**
     * [text] with [import] among its imports, at the top if it has none.
     */
    fun withImport(text: String, import: String): String {
        val lines = text.lines()
        if (import in lines) return text
        val lastImport = lines.indexOfLast { it.startsWith("import ") }
        return if (lastImport >= 0) {
            (lines.take(lastImport + 1) + import + lines.drop(lastImport + 1)).joinToString("\n")
        } else {
            (listOf(import, "") + lines).joinToString("\n")
        }
    }

    /**
     * [text] without [import] if nothing else in it names [name], and without the empty line it leaves at the top.
     */
    fun withoutUnusedImport(text: String, import: String, name: String): String {
        val lines = text.lines()
        if (lines.filter { it != import }.any { name in it }) return text
        val remaining = lines.filter { it != import }
        return (if (lines.firstOrNull() == import) remaining.dropWhile { it.isBlank() } else remaining).joinToString("\n")
    }

    /**
     * [text] with [line] added at its end (after an empty line if [isSeparated]) if no line of it is [line] already, trimmed.
     */
    fun withLine(text: String, line: String, isSeparated: Boolean = false) = when {
        text.lines().any { it.trim() == line } -> text
        text.isBlank() -> "$line\n"
        else -> text.trimEnd() + (if (isSeparated) "\n\n" else "\n") + line + "\n"
    }

    /**
     * [text] without its lines that are [line], trimmed, and without the empty lines that leaves at its end.
     */
    fun withoutLine(text: String, line: String): String {
        if (text.lines().none { it.trim() == line }) return text
        val remaining = text.lines().filterNot { it.trim() == line }.joinToString("\n").trimEnd()
        return if (remaining.isEmpty()) "" else remaining + "\n"
    }
}
