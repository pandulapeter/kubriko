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
import com.pandulapeter.kubrikoCreator.actors.BlockTypeStore
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeCodeGenerator
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
 * Version 2 passes the generated Manager types to Kubriko: `Game.kt` adds `KubrikoWiring.createManagers()` to
 * `Kubriko.newInstance`, and `GameEntryPoint.kt` hands them to the Scene editor. A file is only changed when it still
 * looks like the template's (one `Kubriko.newInstance` with the `SceneLoader` in it, one `createSerializationManager`);
 * a file that already uses `KubrikoWiring` counts as done, whoever changed it.
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
        val steps = listOf(
            "$packagePath/Game.kt" to { text: String -> withManagersCreated(text, wiringImport) },
            "$packagePath/GameEntryPoint.kt" to { text: String -> withManagersHandedToTheSceneEditor(text, wiringImport) },
        )
        val changes = mutableListOf<ProposedFileChange>()
        val byHand = mutableListOf<String>()
        steps.forEach { (path, change) ->
            val file = File(rootDirectory, path)
            val text = if (file.isFile) file.readText() else null
            if (text == null || ManagerTypeCodeGenerator.WIRING_NAME !in text) {
                val changed = text?.let(change)
                if (text != null && changed != null) changes += ProposedFileChange(path, text, changed) else byHand += path
            }
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
                if (file.readText() == change.before) file.writeText(change.after)
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

    private fun recordedVersion() = readSettings()?.let { it["templateVersion"]?.jsonPrimitive?.intOrNull ?: 1 }

    private fun readSettings() = settingsFile.takeIf { it.isFile }?.let { runCatching { json.parseToJsonElement(it.readText()).jsonObject }.getOrNull() }

    private fun recordVersion() {
        val settings = readSettings() ?: return
        settingsFile.writeText(json.encodeToString(JsonObject.serializer(), JsonObject(settings + ("templateVersion" to JsonPrimitive(CURRENT_VERSION)))) + "\n")
    }

    companion object {
        const val CURRENT_VERSION = 2
        private val json = Json { prettyPrint = true }

        /**
         * [text] (a `Game.kt`) with the generated Managers added before the `SceneLoader`, so that they are initialized
         * before the Scene's Actors are added, or null if it doesn't look like the template's.
         */
        internal fun withManagersCreated(text: String, wiringImport: String): String? {
            val lines = text.lines()
            if (lines.count { "Kubriko.newInstance(" in it } != 1) return null
            val start = lines.indexOfFirst { "Kubriko.newInstance(" in it }
            val sceneLoaders = lines.indices.filter { it > start && lines[it].trimStart().startsWith("SceneLoader(") }
            if (sceneLoaders.size != 1) return null
            val sceneLoader = sceneLoaders.single()
            val indent = lines[sceneLoader].takeWhile { it == ' ' }
            val withManagers = lines.toMutableList().apply { add(sceneLoader, "$indent*${ManagerTypeCodeGenerator.WIRING_NAME}.createManagers().toTypedArray(),") }
            return withImport(withManagers, wiringImport)
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
