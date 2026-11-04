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

/**
 * Template version 12: a browser started under the POSIX locale reports its language as `en-US@posix`, which isn't a
 * language tag, and Compose for Web then draws no text at all, so the web `Main.kt` hands Compose the languages without
 * such a modifier before anything is drawn. A `Main.kt` whose `main` isn't the template's block is left to the user.
 */
internal object WebLocaleUpgrade {
    private const val VERSION = 12
    private const val FUNCTION = "useValidBrowserLanguage"
    private const val MAIN = "fun main() {"

    fun isDone(text: String) = FUNCTION in text

    /**
     * [text] (the web `Main.kt`) calling [FUNCTION] first, or null if its `fun main() {` isn't there once.
     */
    fun web(text: String): String? {
        val lines = text.lines()
        val main = lines.indices.singleOrNull { lines[it] == MAIN } ?: return null
        val last = lines.indexOfLast { it.isNotBlank() }
        return (lines.subList(0, main + 1) + "    $FUNCTION()" + lines.subList(main + 1, last + 1) + guard + lines.subList(last + 1, lines.size)).joinToString("\n")
    }

    private val guard = listOf(
        "",
        "/**",
        " * Hands Compose the browser's languages without a modifier such as `@posix`, which a browser started under the POSIX",
        " * locale reports (`en-US@posix`): Compose reads them as language tags, and draws no text at all when one isn't.",
        " */",
        "private fun $FUNCTION(): Unit = js(\"\"\"{",
        "    const isValid = (tag) => { try { new Intl.Locale(tag); return true; } catch (e) { return false; } };",
        "    const reported = navigator.languages || [navigator.language];",
        "    if (!reported.every(isValid)) {",
        "        const valid = reported.map((tag) => tag.split('@')[0]).filter(isValid);",
        "        const languages = valid.length > 0 ? valid : ['en-US'];",
        "        Object.defineProperty(navigator, 'languages', { get: () => languages });",
        "        Object.defineProperty(navigator, 'language', { get: () => languages[0] });",
        "    }",
        "}\"\"\")",
    )

    /**
     * This version's step of the web `Main.kt`.
     */
    object WebStep : TemplateStep {
        override val version = VERSION

        override fun isDone(text: String, context: TemplateContext) = isDone(text)

        override fun change(text: String, context: TemplateContext) = web(text)
    }
}
