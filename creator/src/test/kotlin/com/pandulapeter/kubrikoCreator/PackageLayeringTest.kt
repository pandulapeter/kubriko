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

import com.pandulapeter.kubrikoCreator.testing.creatorDirectory
import com.pandulapeter.kubrikoCreator.testing.creatorSourceRoots
import com.pandulapeter.kubrikoCreator.testing.creatorTestRoots
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Holds Kubriko Creator's packages to the layers of the Gradle modules they are split into, lowest first: an import may only
 * run to the same or a lower layer, and each file sits in the module of its layer. Gradle already refuses an import of a
 * module a module doesn't depend on; this catches an upward import inside one module and a file in the wrong module. Only the
 * screens' entry files ([entryFiles]) take the app, workspace, or assistant controller.
 *
 * [layers] mirrors the `:creator:*` modules of `settings.gradle.kts`, the app (`:creator`) last.
 *
 * [knownExceptions] is empty, kept as the place for an upward import that has to stay for a while, with the plan that removes
 * it. A new upward import is fixed where it is written (by moving the declaration down or by passing it in), never added
 * here without such a plan.
 */
class PackageLayeringTest {
    private val layers = listOf(
        Layer("foundation", "foundation", "console"),
        Layer("ui-components", "ui.components", "ui.theme"),
        Layer("core", "schema", "actors", "actors.codegen", "actors.store", "project", "scenes", "screenplay", "sprites"),
        Layer("gradle-session", "gradle", "session"),
        Layer("git", "git"),
        Layer("template", "template"),
        Layer("commands", "commands", "context"),
        Layer("ai", "ai", "mcp"),
        Layer("scene-runtime", "sceneRuntime"),
        Layer("workspace", "workspace"),
        Layer("app", "", "ui", "settings", "recent", "window", "benchmark", "endToEnd"),
    )
    private val knownExceptions = emptyMap<String, String>()

    /**
     * The files of the app's `ui` packages that may take the app, workspace, or assistant controller: the screens' entry files,
     * which hand every other part the holders, state, and callbacks it uses. It never grows to admit a part.
     */
    private val entryFiles = setOf("ui/AppContent.kt", "ui/WorkspaceScreen.kt")

    /**
     * The controllers only [entryFiles] import, as their imports name them (relative to `com.pandulapeter.kubrikoCreator`).
     */
    private val controllers = setOf("AppController", "workspace.WorkspaceController", "ai.assistant.AssistantController")

    /**
     * The order of the packages inside the core layer's `actors`: the Block model, the code generated from it, and the files
     * it is stored in. Only main sources are held to it, since the model's tests check it against the generated code.
     */
    private val actorsSublayers = listOf("actors", "actors.codegen", "actors.store")
    private val mainSources by lazy { creatorSourceRoots().flatMap(::sourceFilesIn) }
    private val sources by lazy { mainSources + creatorTestRoots().flatMap(::sourceFilesIn) }

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

    @Test
    fun everyModuleHoldsOnlyItsLayer() {
        val misplaced = sources.filter { it.module != APP_MODULE && layerOf(it)?.module != it.module }
            .map { "${it.module}: ${it.path} (${layerOf(it)?.module})" }
        assertTrue(misplaced.isEmpty(), "Files in the module of another layer:\n${misplaced.joinToString("\n")}")
    }

    /**
     * Main sources only, since a test of what the app bundles (`CreatorLibrariesBundlingTest`) belongs to the app.
     */
    @Test
    fun noExtractedLayerIsLeftInTheApp() {
        val extractedModules = sources.map { it.module }.toSet() - APP_MODULE
        val leftBehind = mainSources.filter { it.module == APP_MODULE && layerOf(it)?.module in extractedModules }
            .map { "${it.path} (${layerOf(it)?.module})" }
        assertTrue(leftBehind.isEmpty(), "Files left in the app whose layer has a module of its own:\n${leftBehind.joinToString("\n")}")
    }

    @Test
    fun theActorsModelDoesNotImportCodegenOrStore() {
        val violations = mainSources.flatMap { source ->
            val sourceIndex = actorsSublayers.indexOf(source.unit)
            if (sourceIndex < 0) return@flatMap emptyList()
            source.imports.filter { actorsSublayers.indexOf(it.unit) > sourceIndex }.map { "${source.path} -> ${it.name}" }
        }
        assertTrue(violations.isEmpty(), "Imports that run from the actors model to code generation or storage:\n${violations.joinToString("\n")}")
    }

    /**
     * A `ui` part handed a whole controller can reach everything in it and can only be tested with a real one, so only the
     * [entryFiles] import one. Star imports need no check: ktlint's no-wildcard-imports rule refuses them.
     */
    @Test
    fun onlyEntryFilesTakeTheControllers() {
        val violations = mainSources
            .filter { it.module == APP_MODULE && (it.unit == "ui" || it.unit.startsWith("ui.")) && it.path !in entryFiles }
            .flatMap { source -> source.imports.filter { it.name in controllers }.map { "${source.path} -> ${it.name}" } }
        assertTrue(violations.isEmpty(), "Parts that take a controller; pass them the state and callbacks they use instead:\n${violations.joinToString("\n")}")
    }

    private fun sourceFilesIn(root: File): List<SourceFile> {
        val base = File(root, "com/pandulapeter/kubrikoCreator")
        val moduleDirectory = root.resolve("../../..").canonicalFile
        val module = if (moduleDirectory == creatorDirectory().canonicalFile) APP_MODULE else moduleDirectory.name
        return root.walkTopDown().filter { it.extension == "kt" }.mapNotNull { file ->
            val text = file.readText()
            val unit = packagePattern.find(text)?.let { it.groupValues[1] } ?: return@mapNotNull null
            SourceFile(
                module = module,
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

    private class SourceFile(val module: String, val path: String, val unit: String, val imports: List<Import>)

    private class Import(val name: String, val unit: String, val className: String?)

    private companion object {
        const val APP_MODULE = "app"
        val packagePattern = Regex("""^package com\.pandulapeter\.kubrikoCreator(?:\.([\w.]+))?\s*$""", RegexOption.MULTILINE)
        val importPattern = Regex("""^import com\.pandulapeter\.kubrikoCreator\.([\w.]+?)(\.\*)?(?:\s+as\s+\w+)?\s*$""", RegexOption.MULTILINE)
    }
}
