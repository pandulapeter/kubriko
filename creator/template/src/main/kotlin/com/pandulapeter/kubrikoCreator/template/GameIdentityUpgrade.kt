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

import com.pandulapeter.kubrikoCreator.project.GameIdentity
import com.pandulapeter.kubrikoCreator.project.GradleProperties

/**
 * Template version 9: the game's name and version as keys in `gradle.properties`, which the build file reads for the
 * installers, the generated `GameInfo` object (the desktop window's title), and the web page's title.
 */
internal object GameIdentityUpgrade {
    private const val VERSION = 9
    private const val BLOCK_TEMPLATE = "upgrades/GameIdentity-9.gradle.kts.template"
    private const val WINDOW_TITLE = "title = GameInfo.NAME,"
    private const val PAGE_TITLE = "<title>@GAME_NAME@</title>"
    private val titleLinePattern = Regex("""^(\s*)title = "((?:[^"\\$]|\\.)*)",\s*$""")
    private val pageTitlePattern = Regex("""^(\s*)<title>(.*)</title>\s*$""")
    private val keys = listOf(GameIdentity.NAME, GameIdentity.VERSION_NAME, GameIdentity.VERSION_NUMBER, GameIdentity.APPLICATION_ID, GameIdentity.VENDOR, GameIdentity.DESCRIPTION)

    fun isPropertiesDone(text: String) = keys.all { GradleProperties.valueOf(text, it) != null }

    /**
     * [text] (a `gradle.properties`) with the identity keys it lacks, the game named [name].
     */
    fun properties(text: String, name: String, packageName: String): String {
        val defaults = mapOf(
            GameIdentity.NAME to name,
            GameIdentity.VERSION_NAME to GameIdentity.DEFAULT_VERSION_NAME,
            GameIdentity.VERSION_NUMBER to "1",
            GameIdentity.APPLICATION_ID to packageName,
            GameIdentity.VENDOR to "",
            GameIdentity.DESCRIPTION to "",
        )
        return keys.filter { GradleProperties.valueOf(text, it) == null }.fold(text) { result, key -> GradleProperties.withValue(result, key, defaults.getValue(key)) }
    }

    fun isBuildFileDone(text: String) = GameIdentity.NAME in text

    /**
     * [text] (a `build.gradle.kts`) ending with the block that reads the identity, or null if it doesn't have the
     * template's desktop application and web target, which the block configures.
     */
    fun buildFile(text: String, packageName: String): String? {
        if (isBuildFileDone(text)) return text
        if ("compose.desktop" !in text || "wasmJs" !in text) return null
        val block = TemplateRenderer.renderContent(TemplateFiles.readResource(BLOCK_TEMPLATE), mapOf("PACKAGE" to packageName))
        return text.trimEnd() + "\n\n" + block
    }

    /**
     * Whether [text] (a desktop `Main.kt`) needs nothing: it takes its title from `GameInfo`, or its title isn't the
     * template's single text, which only the user can change.
     */
    fun isMainDone(text: String) = WINDOW_TITLE in text || titleLineOf(text.lines()) == null

    /**
     * [text] (a desktop `Main.kt`) with its window titled from `GameInfo`, or [text] itself if its title isn't the template's.
     */
    fun main(text: String): String {
        val lines = text.lines()
        val line = titleLineOf(lines) ?: return text
        val indent = titleLinePattern.find(lines[line])!!.groupValues[1]
        return (lines.take(line) + "$indent$WINDOW_TITLE" + lines.drop(line + 1)).joinToString("\n")
    }

    /**
     * The window title [text] (a desktop `Main.kt`) sets as plain text, or null if it doesn't.
     */
    fun windowTitleOf(text: String): String? {
        val lines = text.lines()
        return titleLineOf(lines)?.let { titleLinePattern.find(lines[it])!!.groupValues[2].replace(Regex("""\\(.)"""), "$1") }
    }

    fun isPageDone(text: String) = PAGE_TITLE in text || pageTitleLineOf(text.lines()) == null

    /**
     * [text] (the web `index.html`) titled with the game's name when the build copies it, or [text] itself if it has no single title line.
     */
    fun page(text: String): String {
        val lines = text.lines()
        val line = pageTitleLineOf(lines) ?: return text
        val indent = pageTitlePattern.find(lines[line])!!.groupValues[1]
        return (lines.take(line) + "$indent$PAGE_TITLE" + lines.drop(line + 1)).joinToString("\n")
    }

    /**
     * The title [text] (the web `index.html`) has, or null if it doesn't have one.
     */
    fun pageTitleOf(text: String): String? {
        val lines = text.lines()
        return pageTitleLineOf(lines)?.let { pageTitlePattern.find(lines[it])!!.groupValues[2] }
            ?.takeUnless { it == "@GAME_NAME@" }
            ?.replace("&lt;", "<")?.replace("&gt;", ">")?.replace("&amp;", "&")
    }

    /**
     * This version's step of the desktop `Main.kt`.
     */
    object MainStep : TemplateStep {
        override val version = VERSION

        override fun isDone(text: String, context: TemplateContext) = isMainDone(text)

        override fun change(text: String, context: TemplateContext) = main(text)
    }

    /**
     * This version's step of `build.gradle.kts`.
     */
    object BuildFileStep : TemplateStep {
        override val version = VERSION

        override fun isDone(text: String, context: TemplateContext) = isBuildFileDone(text)

        override fun change(text: String, context: TemplateContext) = buildFile(text, context.packageName)
    }

    /**
     * This version's step of the web `index.html`.
     */
    object PageStep : TemplateStep {
        override val version = VERSION

        override fun isDone(text: String, context: TemplateContext) = isPageDone(text)

        override fun change(text: String, context: TemplateContext) = page(text)
    }

    /**
     * This version's step of `gradle.properties`, naming the game as [TemplateContext.gameName] finds it.
     */
    object PropertiesStep : TemplateStep {
        override val version = VERSION

        override fun isDone(text: String, context: TemplateContext) = isPropertiesDone(text)

        override fun change(text: String, context: TemplateContext) = properties(text, context.gameName, context.packageName)
    }

    private fun titleLineOf(lines: List<String>) = lines.indices.filter { titleLinePattern.matches(lines[it]) }.singleOrNull()

    private fun pageTitleLineOf(lines: List<String>) = lines.indices.filter { pageTitlePattern.matches(lines[it]) }.singleOrNull()
}
