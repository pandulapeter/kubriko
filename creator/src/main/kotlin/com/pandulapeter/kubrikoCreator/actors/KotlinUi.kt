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
internal data class KotlinUi(
    val imports: List<String>,
    val statements: List<String>,
) {
    private val body = statements.joinToString("\n")

    /**
     * Whether the statements mention [identifier] as a whole word, so that the generated class has to declare it.
     */
    fun names(identifier: String) = Regex("(?<![\\w$])${Regex.escape(identifier)}(?![\\w$])").containsMatchIn(body)

    companion object {

        /**
         * The Kotlin UI written as [code], or null if it has no statements.
         */
        fun of(code: String?): KotlinUi? {
            val lines = code.orEmpty().lines().map { it.trimEnd() }
            val headerSize = lines.indexOfFirst { it.isNotBlank() && !it.trimStart().startsWith(IMPORT) }.takeIf { it >= 0 } ?: lines.size
            val imports = lines.take(headerSize).filter { it.isNotBlank() }.map { it.trim().removePrefix(IMPORT).trim() }.filter { it.isNotEmpty() }
            val statements = lines.drop(headerSize).dropLastWhile { it.isEmpty() }
            return if (statements.isEmpty()) null else KotlinUi(imports, statements)
        }

        private const val IMPORT = "import "
    }
}
