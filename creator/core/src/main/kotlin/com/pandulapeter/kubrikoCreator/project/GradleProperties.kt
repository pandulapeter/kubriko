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

import java.io.StringReader
import java.util.Properties

/**
 * Line-based edits of single keys of a `gradle.properties` file, which leave every other line as it was. Gradle reads the
 * file as ISO-8859-1, so values are written with `\u` escapes for anything outside ASCII.
 */
public object GradleProperties {

    /**
     * The value of [key] in [text], or null if it isn't set. A value continued on the next line is read whole, and a key
     * set more than once has its last value, as Gradle reads it.
     */
    public fun valueOf(text: String, key: String): String? {
        val lines = text.lines()
        val index = lineOf(lines, key) ?: return null
        val entry = buildString {
            var line = index
            while (line < lines.size) {
                append(lines[line]).append('\n')
                if (!lines[line].continuesOnNextLine()) break
                line++
            }
        }
        return Properties().apply { load(StringReader(entry)) }.getProperty(key)
    }

    /**
     * [text] with [key] set to [value]: its line replaced where Gradle reads it (with any continuation lines), or added at
     * the end.
     */
    public fun withValue(text: String, key: String, value: String): String {
        val lines = text.lines().toMutableList()
        val line = "$key=${escape(value)}"
        val index = lineOf(lines, key) ?: -1
        if (index < 0) {
            val content = lines.dropLastWhile { it.isBlank() }
            return (content + line).joinToString("\n", postfix = "\n")
        }
        var end = index
        while (end < lines.lastIndex && lines[end].continuesOnNextLine()) end++
        repeat(end - index) { lines.removeAt(index + 1) }
        lines[index] = line
        return lines.joinToString("\n")
    }

    /**
     * The index of the last line that sets [key], skipping the lines that continue a value, or null if none does.
     */
    private fun lineOf(lines: List<String>, key: String): Int? {
        var found: Int? = null
        var isContinuation = false
        lines.forEachIndexed { index, line ->
            if (!isContinuation && keyOf(line) == key) found = index
            isContinuation = (isContinuation || keyOf(line) != null) && line.continuesOnNextLine()
        }
        return found
    }

    private fun keyOf(line: String): String? {
        val trimmed = line.trimStart()
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) return null
        val end = trimmed.indexOfFirst { it == '=' || it == ':' || it.isWhitespace() }.takeIf { it >= 0 } ?: trimmed.length
        return trimmed.substring(0, end)
    }

    private fun String.continuesOnNextLine() = takeLastWhile { it == '\\' }.length % 2 == 1

    /**
     * [value] as a `gradle.properties` line holds it after the `=`.
     */
    public fun escape(value: String): String = buildString {
        value.forEachIndexed { index, character ->
            when {
                character == '\\' -> append("\\\\")
                character == '\n' -> append("\\n")
                character == '\r' -> append("\\r")
                character == '\t' -> append("\\t")
                character == ' ' && index == 0 -> append("\\ ")
                character.code < 0x20 || character.code > 0x7E -> append("\\u%04X".format(character.code))
                else -> append(character)
            }
        }
    }
}
