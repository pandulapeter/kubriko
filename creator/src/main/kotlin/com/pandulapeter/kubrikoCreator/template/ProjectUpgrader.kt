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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeCodeGenerator
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator
import com.pandulapeter.kubrikoCreator.actors.BlockTypeStore
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeCodeGenerator
import com.pandulapeter.kubrikoCreator.actors.ScenesCodeGenerator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Brings the template files of an opted-in project (which belong to the user after creation) up to what this version of
 * Kubriko Creator needs, recorded as the `templateVersion` of `.kubriko/project.json`.
 *
 * Version 2 passes the generated Manager types to Kubriko and to the Scene editor (`GameEntryPoint.kt`). Version 3 lets
 * the generated `GeneratedScenes` load the first Scene, so that Blocks can load others: `Game.kt` hands
 * `KubrikoWiring.createManagers` the Scene its `SceneLoader` loaded instead, and `SceneLoader.kt` is deleted if it is
 * still the template's. A file is only changed when it still looks like the template's (one `Kubriko.newInstance` with a
 * one-line `SceneLoader` in it, one `createSerializationManager`); a file that already does what the version needs counts
 * as done, whoever changed it, and so does a `SceneLoader.kt` changed by hand, which is left alone.
 */
internal class ProjectUpgrader(
    private val rootDirectory: File,
) {
    private val settingsFile get() = File(rootDirectory, BlockTypeStore.SETTINGS_FILE)

    fun check(): TemplateUpgrade {
        val settings = readSettings() ?: return TemplateUpgrade.UpToDate
        if ((recordedVersion() ?: CURRENT_VERSION) >= CURRENT_VERSION) return TemplateUpgrade.UpToDate
        val packageName = settings["packageName"]?.jsonPrimitive?.content ?: return TemplateUpgrade.UpToDate
        val sourceDirectory = settings["sourceDirectory"]?.jsonPrimitive?.content ?: "src/commonMain/kotlin"
        val packagePath = "$sourceDirectory/${packageName.replace('.', '/')}"
        val wiringImport = "import $packageName.${ActorTypeCodeGenerator.GENERATED_PACKAGE}.${ManagerTypeCodeGenerator.WIRING_NAME}"
        val changes = mutableListOf<ProposedFileChange>()
        val byHand = mutableListOf<String>()
        fun propose(path: String, isDone: (String) -> Boolean, change: (String) -> String?): Boolean {
            val text = read(path)
            if (text != null && isDone(text)) return true
            val changed = text?.let(change)
            if (text != null && changed != null) changes += ProposedFileChange(path, text, changed) else byHand += path
            return changed != null
        }
        val isGameUpgraded = propose("$packagePath/$GAME_FILE", ::loadsScenesThroughWiring) { withScenesLoadedByWiring(it, wiringImport) }
        propose("$packagePath/$ENTRY_POINT_FILE", { ManagerTypeCodeGenerator.WIRING_NAME in it }) { withManagersHandedToTheSceneEditor(it, wiringImport) }
        val sceneLoaderPath = "$packagePath/$SCENE_LOADER_FILE"
        read(sceneLoaderPath)?.takeIf { isGameUpgraded && it.trimEnd() == templateSceneLoader(packageName)?.trimEnd() }?.let { text ->
            changes += ProposedFileChange(sceneLoaderPath, text, after = null)
        }
        return if (changes.isEmpty() && byHand.isEmpty()) TemplateUpgrade.UpToDate else TemplateUpgrade.Needed(changes, byHand)
    }

    /**
     * Makes every change [check] proposes, if the files haven't changed since, and records the new template version once
     * nothing is left to do by hand.
     *
     * @return What is still left to do.
     */
    fun apply(): TemplateUpgrade {
        val upgrade = check()
        if (upgrade is TemplateUpgrade.Needed) {
            upgrade.changes.forEach { change ->
                val file = File(rootDirectory, change.path)
                if (file.readText() == change.before) {
                    if (change.after == null) file.delete() else file.writeText(change.after)
                }
            }
        }
        return check().also { if (it == TemplateUpgrade.UpToDate) recordVersion() }
    }

    /**
     * Records the current template version when the files need nothing more, as in a project upgraded by hand.
     */
    fun recordVersionIfUpToDate() {
        if (recordedVersion()?.let { it < CURRENT_VERSION } == true && check() == TemplateUpgrade.UpToDate) recordVersion()
    }

    private fun read(path: String) = File(rootDirectory, path).takeIf { it.isFile }?.readText()

    private fun recordedVersion() = readSettings()?.let { it["templateVersion"]?.jsonPrimitive?.intOrNull ?: 1 }

    private fun readSettings() = settingsFile.takeIf { it.isFile }?.let { runCatching { json.parseToJsonElement(it.readText()).jsonObject }.getOrNull() }

    private fun recordVersion() {
        val settings = readSettings() ?: return
        settingsFile.writeText(json.encodeToString(JsonObject.serializer(), JsonObject(settings + ("templateVersion" to JsonPrimitive(CURRENT_VERSION)))) + "\n")
    }

    companion object {
        const val CURRENT_VERSION = 3
        private const val GAME_FILE = "Game.kt"
        private const val ENTRY_POINT_FILE = "GameEntryPoint.kt"
        private const val SCENE_LOADER_FILE = "SceneLoader.kt"
        private const val SCENE_LOADER_TEMPLATE = "upgrades/SceneLoader-2.kt.template"
        private val json = Json { prettyPrint = true }
        private val scenePathPattern = Regex("\"${ScenesCodeGenerator.SCENE_DIRECTORY}/([^\"/]+)\\.json\"")

        /**
         * Whether [text] (a `Game.kt`) passes the generated Managers to Kubriko without a `SceneLoader` of its own.
         */
        internal fun loadsScenesThroughWiring(text: String) = ManagerTypeCodeGenerator.WIRING_NAME in text && text.lines().none { it.isSceneLoader }

        /**
         * [text] (a `Game.kt`) with its `SceneLoader` replaced by the generated Managers loading the same first Scene (and
         * any generated Managers it created already dropped, since they are created again there), or null if it doesn't
         * look like the template's.
         */
        internal fun withScenesLoadedByWiring(text: String, wiringImport: String): String? {
            val lines = text.lines()
            if (lines.count { "Kubriko.newInstance(" in it } != 1) return null
            val start = lines.indexOfFirst { "Kubriko.newInstance(" in it }
            val sceneLoaders = lines.indices.filter { it > start && lines[it].isSceneLoader }
            if (sceneLoaders.size != 1) return null
            val sceneLoader = sceneLoaders.single()
            if (!lines[sceneLoader].trimEnd().endsWith("),")) return null
            val firstScene = scenePathPattern.find(lines[sceneLoader])?.groupValues?.get(1) ?: return null
            val indent = lines[sceneLoader].takeWhile { it == ' ' }
            val wiring = "*${ManagerTypeCodeGenerator.WIRING_NAME}.createManagers("
            val changed = lines.mapIndexedNotNull { index, line ->
                when {
                    index == sceneLoader -> "$indent${wiring}firstScene = ${BlockCodeGenerator.stringLiteral(firstScene)}).toTypedArray(),"
                    index > start && line.trim() == "$wiring).toTypedArray()," -> null
                    else -> line
                }
            }
            return if (wiringImport in changed) changed.joinToString("\n") else withImport(changed, wiringImport)
        }

        /**
         * [text] (a `GameEntryPoint.kt`) handing the generated Managers to the Scene editor, or null if it doesn't look
         * like the template's.
         */
        internal fun withManagersHandedToTheSceneEditor(text: String, wiringImport: String): String? {
            val lines = text.lines()
            if ("createCustomManagers" in text) return null
            val serializationLines = lines.indices.filter { lines[it].trimStart().startsWith("override fun createSerializationManager()") && lines[it].trimEnd().endsWith(")") }
            if (serializationLines.size != 1) return null
            val line = serializationLines.single()
            val indent = lines[line].takeWhile { it == ' ' }
            val withOverride = lines.toMutableList().apply {
                addAll(line + 1, listOf("", "${indent}override fun createCustomManagers() = ${ManagerTypeCodeGenerator.WIRING_NAME}.createManagers(isInSceneEditor = true)"))
            }
            return withImport(withOverride, wiringImport)
        }

        /**
         * The `SceneLoader.kt` that templates before version 3 created in [packageName].
         */
        private fun templateSceneLoader(packageName: String) = TemplateFiles.readResource(SCENE_LOADER_TEMPLATE)
            ?.let { TemplateRenderer.renderContent(it, mapOf("PACKAGE" to packageName)) }

        private val String.isSceneLoader get() = trimStart().startsWith("SceneLoader(")

        private fun withImport(lines: List<String>, import: String): String {
            val lastImport = lines.indexOfLast { it.startsWith("import ") }
            val packageLine = lines.indexOfFirst { it.startsWith("package ") }
            val result = lines.toMutableList()
            when {
                lastImport >= 0 -> result.add(lastImport + 1, import)
                packageLine >= 0 -> result.addAll(packageLine + 1, listOf("", import))
                else -> result.addAll(0, listOf(import, ""))
            }
            return result.joinToString("\n")
        }
    }
}
