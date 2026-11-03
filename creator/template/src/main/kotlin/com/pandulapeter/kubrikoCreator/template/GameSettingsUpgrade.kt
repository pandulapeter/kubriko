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

import com.pandulapeter.kubrikoCreator.actors.codegen.ManagerTypeCodeGenerator
import com.pandulapeter.kubrikoCreator.actors.codegen.ScenesCodeGenerator
import com.pandulapeter.kubrikoCreator.project.GameIdentity
import com.pandulapeter.kubrikoCreator.project.GameSettings
import com.pandulapeter.kubrikoCreator.project.GradleProperties

/**
 * Template version 10: the first Scene, the aspect ratio, and the frame rate as keys in `gradle.properties`, which the
 * build file turns into the generated `GameSettings` object that `Game.kt` creates its Kubriko instance from.
 */
internal object GameSettingsUpgrade {
    private const val VERSION = 10
    private const val BLOCK_TEMPLATE = "upgrades/GameSettings-10.gradle.kts.template"
    private const val FIRST_SCENE = "GameSettings.FIRST_SCENE"
    private const val ASPECT_RATIO_MODE = "GameSettings.ASPECT_RATIO_MODE"
    private const val VIEWPORT_IMPORT = "import com.pandulapeter.kubriko.manager.ViewportManager"
    private const val KUBRIKO_IMPORT_PREFIX = "import com.pandulapeter.kubriko."
    private val wiringLinePattern = Regex("""^(\s*)\*${ManagerTypeCodeGenerator.WIRING_NAME}\.createManagers\(firstScene = (?:"([^"\\$]*)"|${Regex.escape(FIRST_SCENE)})\)\.toTypedArray\(\),\s*$""")
    private val sceneLoaderPattern = Regex("""SceneLoader\(scenePath = "${ScenesCodeGenerator.SCENE_DIRECTORY}/([^"\\$]+)\.json"\)""")

    fun isPropertiesDone(text: String) = GameSettings.ALL_KEYS.all { GradleProperties.valueOf(text, it) != null }

    /**
     * [text] (a `gradle.properties`) with the settings keys it lacks, at their defaults but for the [firstScene].
     */
    fun properties(text: String, firstScene: String) = GameSettings(firstScene = firstScene)
        .writtenTo(text, GameSettings.ALL_KEYS.filter { GradleProperties.valueOf(text, it) == null })

    fun isBuildFileDone(text: String) = GameSettings.FIRST_SCENE in text

    /**
     * [text] (a `build.gradle.kts`) ending with the block that turns the settings into `GameSettings`, or null if it
     * isn't a multiplatform build with the common source set the block adds to.
     */
    fun buildFile(text: String, packageName: String): String? {
        if (isBuildFileDone(text)) return text
        if ("commonMain" !in text) return null
        val block = TemplateRenderer.renderContent(TemplateFiles.readResource(BLOCK_TEMPLATE), mapOf("PACKAGE" to packageName))
        return text.trimEnd() + "\n\n" + block
    }

    /**
     * Whether [text] (a `Game.kt`) creates its Kubriko instance from `GameSettings` already.
     */
    fun isGameDone(text: String) = FIRST_SCENE in text && ASPECT_RATIO_MODE in text

    /**
     * [text] (a `Game.kt`) starting with `GameSettings.FIRST_SCENE` and creating its `ViewportManager` from `GameSettings`,
     * or null if it doesn't hand the generated Managers a first Scene in one line, as the template does, or creates a
     * `ViewportManager` of its own.
     */
    fun game(text: String): String? {
        if (isGameDone(text)) return text
        val lines = text.lines()
        val line = lines.indices.filter { wiringLinePattern.matches(lines[it]) }.singleOrNull() ?: return null
        if ("ViewportManager.newInstance(" in text) return null
        val indent = wiringLinePattern.find(lines[line])!!.groupValues[1]
        val viewportManager = listOf(
            "${indent}ViewportManager.newInstance(",
            "$indent    aspectRatioMode = $ASPECT_RATIO_MODE,",
            "$indent    initialTargetFrameRate = GameSettings.TARGET_FRAME_RATE,",
            "$indent),",
            "$indent*${ManagerTypeCodeGenerator.WIRING_NAME}.createManagers(firstScene = $FIRST_SCENE).toTypedArray(),",
        )
        return withViewportImport(lines.take(line) + viewportManager + lines.drop(line + 1))
    }

    /**
     * The Scene [text] (a `Game.kt`) starts with, as the template names it, or null if it doesn't name one.
     */
    fun firstSceneOf(text: String): String? = text.lines().firstNotNullOfOrNull { line ->
        wiringLinePattern.find(line)?.groupValues?.get(2)?.takeIf { it.isNotEmpty() } ?: sceneLoaderPattern.find(line)?.groupValues?.get(1)
    }

    private fun withViewportImport(lines: List<String>): String {
        if (VIEWPORT_IMPORT in lines) return lines.joinToString("\n")
        val after = lines.indexOfLast { it.startsWith(KUBRIKO_IMPORT_PREFIX) }.takeIf { it >= 0 } ?: lines.indexOfLast { it.startsWith("import ") }
        val result = lines.toMutableList()
        if (after >= 0) result.add(after + 1, VIEWPORT_IMPORT) else result.addAll(1, listOf("", VIEWPORT_IMPORT))
        return result.joinToString("\n")
    }

    /**
     * This version's step of `Game.kt`, which waits for the build file's, since `GameSettings` comes from it.
     */
    object GameStep : TemplateStep {
        override val version = VERSION

        override fun isDone(text: String, context: TemplateContext) = isGameDone(text)

        override fun change(text: String, context: TemplateContext): String? {
            val build = context.read(GameIdentity.BUILD_FILE) ?: return null
            return if (buildFile(build, context.packageName) == null) null else game(text)
        }
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
     * This version's step of `gradle.properties`, starting with the Scene `Game.kt` starts with ([TemplateContext.firstScene]).
     */
    object PropertiesStep : TemplateStep {
        override val version = VERSION

        override fun isDone(text: String, context: TemplateContext) = isPropertiesDone(text)

        override fun change(text: String, context: TemplateContext) = properties(text, context.firstScene)
    }
}
