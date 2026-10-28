/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

/**
 * Finds and renames the identifiers of Kotlin code written into Kotlin Blocks and Kotlin UI, so that it can follow the
 * renames Blocks follow. It knows Kotlin's comments, strings, and string templates, not its grammar: a rename changes every
 * matching identifier, a local variable of the same name included.
 */
internal object KotlinIdentifiers {

    /**
     * An identifier in code.
     *
     * @param receiver What comes before it and a dot: null if nothing does, the identifier if it is a plain one itself
     *   (`this` included), and an empty string for anything else, such as a call or a longer chain.
     */
    data class Occurrence(
        val range: IntRange,
        val name: String,
        val receiver: String?,
    )

    fun find(code: String) = Scanner(code).run {
        scanCode(isTemplate = false)
        occurrences.toList()
    }

    /**
     * [code] with [oldName] renamed to [newName] wherever it has no receiver (or `this`), or, if [receiver] is given,
     * wherever it is a member of that plain identifier, as in `receiver.oldName`.
     */
    fun withRenamed(code: String, oldName: String, newName: String, receiver: String? = null): String {
        if (oldName == newName || oldName !in code) return code
        val matches = find(code).filter { it.name == oldName && if (receiver == null) it.receiver == null || it.receiver == THIS else it.receiver == receiver }
        return matches.foldRight(code) { occurrence, renamed -> renamed.replaceRange(occurrence.range, newName) }
    }

    private const val THIS = "this"

    private class Scanner(private val code: String) {
        val occurrences = mutableListOf<Occurrence>()
        private var index = 0

        private fun peek(offset: Int = 0) = code.getOrNull(index + offset)

        private fun startsWith(text: String) = code.startsWith(text, index)

        /**
         * Code until its end, or, in a `${…}` template, until the brace that closes it.
         */
        fun scanCode(isTemplate: Boolean) {
            var depth = 0
            var previous: Occurrence? = null
            var isAfterDot = false
            while (index < code.length) {
                val char = code[index]
                when {
                    char.isIdentifierStart() -> {
                        val start = index
                        while (peek()?.isIdentifierPart() == true) index++
                        val receiver = if (isAfterDot) previous?.takeIf { it.receiver == null }?.name.orEmpty() else null
                        previous = Occurrence(start until index, code.substring(start, index), receiver).also { occurrences += it }
                        isAfterDot = false
                    }
                    char.isDigit() -> {
                        while (peek()?.isIdentifierPart() == true || (peek() == '.' && peek(1)?.isDigit() == true)) index++
                        previous = null
                    }
                    char.isWhitespace() -> index++
                    startsWith("//") -> skipLineComment()
                    startsWith("/*") -> skipBlockComment()
                    startsWith("\"\"\"") -> {
                        index += 3
                        scanString(isRaw = true)
                        previous = null
                    }
                    char == '"' -> {
                        index++
                        scanString(isRaw = false)
                        previous = null
                    }
                    char == '\'' -> {
                        skipCharLiteral()
                        previous = null
                    }
                    (char == '.' && peek(1) != '.') || startsWith("?.") -> {
                        index += if (char == '?') 2 else 1
                        isAfterDot = true
                    }
                    else -> {
                        if (char == '{') depth++
                        if (char == '}') {
                            if (isTemplate && depth == 0) {
                                index++
                                return
                            }
                            depth--
                        }
                        index += if (startsWith("..")) 2 else 1
                        previous = null
                        isAfterDot = false
                    }
                }
            }
        }

        private fun scanString(isRaw: Boolean) {
            while (index < code.length) {
                when {
                    isRaw && startsWith("\"\"\"") -> {
                        index += 3
                        while (peek() == '"') index++
                        return
                    }
                    !isRaw && peek() == '"' -> {
                        index++
                        return
                    }
                    !isRaw && peek() == '\\' -> index += 2
                    !isRaw && peek() == '\n' -> return
                    startsWith("\${") -> {
                        index += 2
                        scanCode(isTemplate = true)
                    }
                    peek() == '$' && peek(1)?.isIdentifierStart() == true -> {
                        val start = ++index
                        while (peek()?.isIdentifierPart() == true) index++
                        occurrences += Occurrence(start until index, code.substring(start, index), null)
                    }
                    else -> index++
                }
            }
        }

        private fun skipLineComment() {
            while (index < code.length && code[index] != '\n') index++
        }

        private fun skipBlockComment() {
            var depth = 0
            while (index < code.length) {
                when {
                    startsWith("/*") -> {
                        depth++
                        index += 2
                    }
                    startsWith("*/") -> {
                        index += 2
                        if (--depth == 0) return
                    }
                    else -> index++
                }
            }
        }

        private fun skipCharLiteral() {
            index++
            if (peek() == '\\') index += 2
            while (index < code.length && code[index] != '\'' && code[index] != '\n') index++
            index++
        }

        private fun Char.isIdentifierStart() = isLetter() || this == '_'

        private fun Char.isIdentifierPart() = isLetterOrDigit() || this == '_'
    }
}
