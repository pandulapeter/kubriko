/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

/**
 * Line-based edits of a Kotlin Multiplatform build file (`build.gradle.kts`), which leave every other line as it was.
 */
public object BuildScript {
    private const val COMMON_DEPENDENCIES = "commonMain.dependencies {"

    /**
     * Whether [text] refers to the version catalog library with [accessor] anywhere.
     */
    public fun refersTo(text: String, accessor: String): Boolean = Regex("""\b${Regex.escape(accessor)}\b(?![.\w])""").containsMatchIn(text)

    /**
     * [text] with an `implementation` line for each of [added] in its common dependencies, and without the lines that
     * do nothing but add one of [removed], wherever they are. New lines go where they keep the lines sorted if they are.
     *
     * @return The changed text, or null if something has to be added but the file has no common dependencies block.
     */
    internal fun withDependencies(text: String, added: Collection<String>, removed: Collection<String>): String? {
        val removedLines = removed.mapTo(mutableSetOf()) { "implementation($it)" } + removed.map { "api($it)" }
        val lines = text.lines().filterNot { it.trim() in removedLines }.toMutableList()
        added.forEach { accessor ->
            val start = lines.indexOfFirst { it.trim() == COMMON_DEPENDENCIES }
            if (start < 0) return null
            val end = blockEnd(lines, start) ?: return null
            val children = (start + 1 until end).filter { lines[it].isNotBlank() }
            val indent = children.firstOrNull()?.let { lines[it].takeWhile(Char::isWhitespace) } ?: (lines[start].takeWhile(Char::isWhitespace) + "    ")
            val line = "implementation($accessor)"
            val existing = children.map { lines[it].trim() }
            val position = when {
                children.isEmpty() -> start + 1
                existing == existing.sorted() -> children.firstOrNull { lines[it].trim() > line } ?: (children.last() + 1)
                else -> children.last() + 1
            }
            lines.add(position, indent + line)
        }
        return lines.joinToString("\n")
    }

    /**
     * The index of the line closing the block opened on the line at [start] (counting parentheses too if
     * [countsParentheses]), or null if it isn't closed. Brackets in string literals and line comments don't count.
     */
    public fun blockEnd(lines: List<String>, start: Int, countsParentheses: Boolean = false): Int? {
        var depth = 0
        for (index in start until lines.size) {
            val code = codeOf(lines[index])
            depth += code.count { it == '{' || (countsParentheses && it == '(') } - code.count { it == '}' || (countsParentheses && it == ')') }
            if (depth <= 0) return index
        }
        return null
    }

    /**
     * [line] without its string literals and its line comment.
     */
    private fun codeOf(line: String) = buildString {
        var isInString = false
        var index = 0
        while (index < line.length) {
            val character = line[index]
            when {
                isInString && character == '\\' -> index++
                character == '"' -> isInString = !isInString
                !isInString && character == '/' && line.getOrNull(index + 1) == '/' -> return@buildString
                !isInString -> append(character)
            }
            index++
        }
    }
}
