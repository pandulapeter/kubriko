/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Holds Kubriko Creator's packages to the layers of the Gradle modules they are split into, lowest first: an import may only
 * run to the same or a lower layer.
 *
 * [knownExceptions] is the work list of the upward imports that were there when this test was written, each with the plan
 * that removes it. It only shrinks: a new upward import is fixed where it is written (by moving the declaration down or by
 * passing it in), never added here.
 */
class PackageLayeringTest {
    private val coreSceneFiles = arrayOf(
        "scenes/SceneStore",
        "scenes/SceneDocument",
        "scenes/SceneActorEntry",
        "scenes/SceneSettingsState",
        "scenes/SceneDescriptions",
        "scenes/SceneNaming",
        "scenes/ActorStateEdits",
        "scenes/SceneFileProblem",
        "scenes/ScenePatch",
        "scenes/SceneChange",
    )
    private val layers = listOf(
        Layer("foundation", "foundation", "console"),
        Layer("ui-components", "ui.components", "ui.theme"),
        Layer("core", "schema", "actors", "project", *coreSceneFiles),
        Layer("gradle-session", "gradle", "session"),
        Layer("git", "git"),
        Layer("template", "template"),
        Layer("commands", "commands", "context"),
        Layer("ai", "ai", "mcp"),
        Layer("scene-runtime", "scenes"),
        Layer("workspace", "workspace"),
        Layer("app", "", "ui", "settings", "recent", "window", "benchmark"),
    )
    private val knownExceptions = mapOf(
        "ui/components/ColorField.kt -> actors.ColorHex" to "M04",
        "ui/components/ColorPickerValue.kt -> actors.ColorHex" to "M04",
        "ui/theme/CreatorTheme.kt -> settings.ThemeChoice" to "M04",
        "ui/components/BufferedTextFieldTest.kt -> ui.showing" to "M04",
        "ui/components/BufferedTextFieldTest.kt -> ui.uiTest" to "M04",
        "ui/components/ColorFieldTest.kt -> ui.saveSnapshot" to "M04",
        "ui/components/ColorFieldTest.kt -> ui.showing" to "M04",
        "ui/components/ColorFieldTest.kt -> ui.uiTest" to "M04",
        "ui/components/LabeledSwitchTest.kt -> ui.saveSnapshot" to "M04",
        "ui/components/LabeledSwitchTest.kt -> ui.showing" to "M04",
        "ui/components/LabeledSwitchTest.kt -> ui.uiTest" to "M04",
        "ui/components/NumberFieldTest.kt -> ui.saveSnapshot" to "M04",
        "ui/components/NumberFieldTest.kt -> ui.showing" to "M04",
        "ui/components/NumberFieldTest.kt -> ui.uiTest" to "M04",
        "ui/components/PanelSplitterTest.kt -> ui.saveSnapshot" to "M04",
        "ui/components/PanelSplitterTest.kt -> ui.showing" to "M04",
        "ui/components/PanelSplitterTest.kt -> ui.uiTest" to "M04",
        "ui/components/SearchableChoiceButtonTest.kt -> ui.saveSnapshot" to "M04",
        "ui/components/SearchableChoiceButtonTest.kt -> ui.showing" to "M04",
        "ui/components/SearchableChoiceButtonTest.kt -> ui.uiTest" to "M04",
        "project/ProjectInspector.kt -> gradle.GradleProjectConnection" to "M06",
        "actors/KotlinIdentifiersTest.kt -> template.ProjectNaming" to "M06",
        "commands/CommandTesting.kt -> workspace.BlockTypesController" to "M06",
        "session/GameLaunchTest.kt -> scenes.ProjectClasspath" to "M06",
        "session/HotReloadLaunchTest.kt -> scenes.ProjectClasspath" to "M06",
        "template/ProjectTemplateIntegrationTest.kt -> scenes.ProjectClasspath" to "M06",
    )
    private val sources by lazy { listOf("src/main/kotlin", "src/test/kotlin").flatMap(::sourceFilesIn) }

    @Test
    fun noImportRunsToAHigherLayer() {
        val violations = sources.flatMap { source ->
            val sourceLayer = layerOf(source) ?: return@flatMap emptyList()
            source.imports.mapNotNull { import ->
                val importLayer = layerOf(import) ?: return@mapNotNull null
                val key = "${source.path} -> ${import.name}"
                key.takeIf { layers.indexOf(importLayer) > layers.indexOf(sourceLayer) && key !in knownExceptions }
                    ?.let { "$it (${sourceLayer.module} -> ${importLayer.module})" }
            }
        }
        assertTrue(violations.isEmpty(), "Imports that run to a higher layer:\n${violations.joinToString("\n")}")
    }

    @Test
    fun everyKnownExceptionStillOccurs() {
        val imports = sources.flatMapTo(HashSet()) { source -> source.imports.map { "${source.path} -> ${it.name}" } }
        val stale = knownExceptions.filterKeys { it !in imports }.map { (key, plan) -> "$key ($plan)" }
        assertTrue(stale.isEmpty(), "Known exceptions that no longer occur, remove them:\n${stale.joinToString("\n")}")
    }

    @Test
    fun everyPackageHasALayer() {
        val unplaced = sources.filter { layerOf(it) == null }.map { it.unit }.distinct().sorted()
        assertTrue(unplaced.isEmpty(), "Packages without a layer:\n${unplaced.joinToString("\n")}")
    }

    @Test
    fun everyLayerHasSources() {
        val usedEntries = sources.mapNotNullTo(HashSet(), ::entryOf)
        val unused = layers.flatMap { layer -> layer.entries.filter { it !in usedEntries }.map { "${layer.module}: \"$it\"" } }
        assertTrue(unused.isEmpty(), "Layer entries that match no source file:\n${unused.joinToString("\n")}")
    }

    private fun sourceFilesIn(rootPath: String): List<SourceFile> {
        val root = File(rootPath)
        assertTrue(root.isDirectory, "Run from the creator folder: ${root.absolutePath}")
        val base = File(root, "com/pandulapeter/kubrikoCreator")
        return root.walkTopDown().filter { it.extension == "kt" }.mapNotNull { file ->
            val text = file.readText()
            val unit = packagePattern.find(text)?.let { it.groupValues[1] } ?: return@mapNotNull null
            SourceFile(
                path = file.relativeTo(base).invariantSeparatorsPath,
                unit = unit,
                imports = importPattern.findAll(text).map { importOf(it.groupValues[1], isStar = it.groupValues[2].isNotEmpty()) }.toList(),
            )
        }.filterNot { it.unit.isTesting() }.toList()
    }

    private fun importOf(name: String, isStar: Boolean): Import {
        val segments = name.split('.')
        val classIndex = segments.indexOfFirst { it.first().isUpperCase() }
        val packageSegments = when {
            isStar -> segments
            classIndex >= 0 -> segments.take(classIndex)
            else -> segments.dropLast(1)
        }
        return Import(name = name, unit = packageSegments.joinToString("."), className = segments.getOrNull(classIndex))
    }

    private fun layerOf(source: SourceFile) = entryOf(source)?.let(::layerOfEntry)

    private fun layerOf(import: Import): Layer? {
        if (import.unit.isTesting()) return null
        val fileEntry = import.className?.let { "${import.unit.replace('.', '/')}/$it" }
        return fileEntry?.let(::layerOfEntry) ?: packageEntryOf(import.unit)?.let(::layerOfEntry)
    }

    private fun entryOf(source: SourceFile): String? {
        val fileEntry = source.path.removeSuffix(".kt")
        return fileEntry.takeIf { layerOfEntry(it) != null && it.substringBeforeLast('/').replace('/', '.') == source.unit }
            ?: packageEntryOf(source.unit)
    }

    private fun packageEntryOf(unit: String) = layers.asSequence()
        .flatMap { it.entries }
        .filter { '/' !in it && (it == unit || (it.isNotEmpty() && unit.startsWith("$it."))) }
        .maxByOrNull { it.length }

    private fun layerOfEntry(entry: String) = layers.firstOrNull { entry in it.entries }

    private fun String.isTesting() = this == "testing" || startsWith("testing.")

    private class Layer(val module: String, vararg entries: String) {
        val entries = entries.toList()
    }

    private class SourceFile(val path: String, val unit: String, val imports: List<Import>)

    private class Import(val name: String, val unit: String, val className: String?)

    private companion object {
        val packagePattern = Regex("""^package com\.pandulapeter\.kubrikoCreator(?:\.([\w.]+))?\s*$""", RegexOption.MULTILINE)
        val importPattern = Regex("""^import com\.pandulapeter\.kubrikoCreator\.([\w.]+?)(\.\*)?(?:\s+as\s+\w+)?\s*$""", RegexOption.MULTILINE)
    }
}
