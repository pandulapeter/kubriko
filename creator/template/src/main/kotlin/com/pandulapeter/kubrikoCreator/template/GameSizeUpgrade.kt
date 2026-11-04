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
 * Template version 13: the web and Android entry points handed `Game` a `Modifier.fillMaxSize()`, which kept a fixed
 * aspect ratio from letterboxing the viewport and the Manager types' texts from drawing, so they call `Game()` as the
 * desktop and iOS ones do: the viewport fills the screen by itself for every other aspect ratio. A file that hands it
 * anything else counts as done, since that is the user's own.
 */
internal object GameSizeUpgrade {
    private const val VERSION = 13
    private const val MODIFIER_LINE = "modifier = Modifier.fillMaxSize(),"
    private val unusedImports = listOf(
        "import androidx.compose.foundation.layout.fillMaxSize" to "fillMaxSize",
        "import androidx.compose.ui.Modifier" to "Modifier",
    )

    fun isDone(text: String) = callLineOf(text.lines()) == null

    /**
     * [text] calling `Game()` where it called `Game(modifier = Modifier.fillMaxSize())` on three lines, without the imports
     * nothing else uses then.
     */
    fun change(text: String): String {
        val lines = text.lines()
        val call = callLineOf(lines) ?: return text
        val changed = (lines.subList(0, call) + "${lines[call].removeSuffix("(")}()" + lines.subList(call + 3, lines.size)).joinToString("\n")
        return unusedImports.fold(changed) { result, (import, name) -> BuildFileLines.withoutUnusedImport(result, import, name) }
    }

    private fun callLineOf(lines: List<String>) = lines.indices.singleOrNull { index ->
        val indent = lines[index].takeWhile { it == ' ' }
        lines[index] == "${indent}Game(" && lines.getOrNull(index + 1) == "$indent    $MODIFIER_LINE" && lines.getOrNull(index + 2) == "$indent)"
    }

    /**
     * This version's step of the web `Main.kt`.
     */
    object WebStep : TemplateStep {
        override val version = VERSION

        override fun isDone(text: String, context: TemplateContext) = isDone(text)

        override fun change(text: String, context: TemplateContext) = change(text)
    }

    /**
     * This version's step of the Android `MainActivity.kt`.
     */
    object AndroidStep : TemplateStep {
        override val version = VERSION

        override fun isDone(text: String, context: TemplateContext) = isDone(text)

        override fun change(text: String, context: TemplateContext) = change(text)
    }
}
