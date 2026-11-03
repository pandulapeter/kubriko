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
 * A Manager type's Kotlin UI, split into the imports its code starts with, which the generated file declares, and the
 * statements that go into the Manager's `Composable`.
 */
public data class KotlinUi(
    val imports: List<String>,
    val statements: List<String>,
) {
    private val body = statements.joinToString("\n")

    /**
     * Whether the statements mention [identifier] as a whole word, so that the generated class has to declare it.
     */
    internal fun names(identifier: String) = Regex("(?<![\\w$])${Regex.escape(identifier)}(?![\\w$])").containsMatchIn(body)

    public companion object {

        /**
         * The Kotlin UI written as [code], or null if it has no statements.
         */
        public fun of(code: String?): KotlinUi? {
            val (imports, statements) = splitImports(code.orEmpty())
            return if (statements.isEmpty()) null else KotlinUi(imports, statements)
        }

        /**
         * The import lines [code] starts with (blank lines among them included), up to its first other line.
         */
        internal fun headerLineCount(code: String): Int {
            val lines = code.lines()
            return lines.indexOfFirst { it.isNotBlank() && !it.trimStart().startsWith(IMPORT) }.takeIf { it >= 0 } ?: lines.size
        }

        /**
         * The imports [code] starts with, without `import ` (an alias kept as written), and its remaining lines, without
         * trailing spaces and trailing blank lines. An import after the first statement stays a statement, for the
         * compiler to report.
         */
        internal fun splitImports(code: String): Pair<List<String>, List<String>> {
            val lines = code.lines().map { it.trimEnd() }
            val headerSize = headerLineCount(code)
            val imports = lines.take(headerSize).filter { it.isNotBlank() }.map { it.trim().removePrefix(IMPORT).trim() }.filter { it.isNotEmpty() }
            return imports to lines.drop(headerSize).dropLastWhile { it.isEmpty() }
        }

        private const val IMPORT = "import "
    }
}
