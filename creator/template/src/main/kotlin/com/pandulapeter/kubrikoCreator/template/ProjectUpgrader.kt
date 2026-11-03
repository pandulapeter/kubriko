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

import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator
import com.pandulapeter.kubrikoCreator.actors.codegen.ManagerTypeCodeGenerator
import com.pandulapeter.kubrikoCreator.actors.codegen.ScenesCodeGenerator
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypeStore
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import com.pandulapeter.kubrikoCreator.project.ProjectInspector
import com.pandulapeter.kubrikoCreator.project.withUnixLineBreaks
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
 * Each version is a [TemplateStep] per file it changes ([TemplateSteps]). A file is only changed when it still looks like
 * the template's, as each step defines; a file that already has a step's change counts as done for it, whoever made it,
 * and a file one of whose steps can't be made is left to the user, with none of its steps made.
 */
public class ProjectUpgrader(
    private val rootDirectory: File,
) {
    private val settingsFile get() = File(rootDirectory, BlockTypeStore.SETTINGS_FILE)

    /**
     * What it takes to bring the project up to [CURRENT_VERSION], without changing anything.
     */
    public fun check(): TemplateUpgrade {
        val settings = readSettings() ?: return TemplateUpgrade.UpToDate
        if ((recordedVersion() ?: CURRENT_VERSION) >= CURRENT_VERSION) return TemplateUpgrade.UpToDate
        val packageName = settings["packageName"]?.jsonPrimitive?.content ?: return TemplateUpgrade.UpToDate
        val sourceDirectory = settings["sourceDirectory"]?.jsonPrimitive?.content ?: "src/commonMain/kotlin"
        val context = TemplateContext(packageName, sourceDirectory, ::read) { ProjectInspector.nameOf(rootDirectory) }
        val changes = mutableListOf<ProposedFileChange>()
        val byHand = mutableListOf<String>()
        val doneOrProposedPaths = HashSet<String>()
        TemplateSteps.FILES.forEach { file ->
            val path = file.path(context)
            val text = context.read(path) ?: when (file.whenMissing) {
                UpgradedFile.WhenMissing.BY_HAND -> {
                    byHand += path
                    return@forEach
                }
                UpgradedFile.WhenMissing.SKIP -> return@forEach
                UpgradedFile.WhenMissing.CREATE_EMPTY -> ""
            }
            file.deletion?.takeIf { it.requiredPath(context) in doneOrProposedPaths && it.isTemplates(text, context) }?.let {
                changes += ProposedFileChange(path, text, after = null)
            }
            var upgraded: String? = text
            var isChanged = false
            for (step in file.steps) {
                val current = upgraded ?: break
                if (!step.isDone(current, context)) {
                    upgraded = step.change(current, context)
                    isChanged = true
                }
            }
            when {
                !isChanged -> doneOrProposedPaths += path
                upgraded == null -> byHand += path
                else -> {
                    changes += ProposedFileChange(path, text, upgraded)
                    doneOrProposedPaths += path
                }
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
    @EditorMutation
    public fun apply(): TemplateUpgrade {
        val upgrade = check()
        if (upgrade is TemplateUpgrade.Needed) {
            upgrade.changes.forEach { change ->
                val file = File(rootDirectory, change.path)
                val raw = if (file.isFile) file.readText() else ""
                if (raw.withUnixLineBreaks() == change.before) change.writeTo(file, raw)
            }
        }
        return check().also { if (it == TemplateUpgrade.UpToDate) recordVersion() }
    }

    /**
     * Records the current template version when the files need nothing more, as in a project upgraded by hand.
     */
    public fun recordVersionIfUpToDate() {
        if (recordedVersion()?.let { it < CURRENT_VERSION } == true && check() == TemplateUpgrade.UpToDate) recordVersion()
    }

    /**
     * The package the project's template files are in, or null if it isn't opted in.
     */
    internal val packageName: String? get() = readSettings()?.get("packageName")?.jsonPrimitive?.content

    private fun read(path: String) = File(rootDirectory, path).takeIf { it.isFile }?.readText()?.withUnixLineBreaks()

    private fun recordedVersion() = readSettings()?.let { it["templateVersion"]?.jsonPrimitive?.intOrNull ?: 1 }

    private fun readSettings() = settingsFile.takeIf { it.isFile }?.let { runCatching { json.parseToJsonElement(it.readText()).jsonObject }.getOrNull() }

    private fun recordVersion() {
        val settings = readSettings() ?: return
        settingsFile.writeTextAtomically(json.encodeToString(JsonObject.serializer(), JsonObject(settings + ("templateVersion" to JsonPrimitive(CURRENT_VERSION)))) + "\n")
    }

    public companion object {
        /**
         * The template version the newest [TemplateStep] brings a project to, which new projects start at.
         */
        public val CURRENT_VERSION: Int = TemplateSteps.FILES.flatMap { it.steps }.maxOf { it.version }
        private val cacheProperties = listOf("org.gradle.configuration-cache", "org.gradle.caching")
        private const val APPLICATION_MAIN = "fun main() = application {"
        private const val LOGGING_SWITCH = "${ManagerTypeCodeGenerator.WIRING_NAME}.${ManagerTypeCodeGenerator.LOGGING_FLAG}"
        private const val FIRST_SCENE_SWITCH = "${ManagerTypeCodeGenerator.WIRING_NAME}.${ManagerTypeCodeGenerator.FIRST_SCENE_FLAG}"
        private const val SCENE_LOADER_TEMPLATE = "upgrades/SceneLoader-2.kt.template"
        private val json = Json { prettyPrint = true }
        private val scenePathPattern = Regex("\"${ScenesCodeGenerator.SCENE_DIRECTORY}/([^\"/]+)\\.json\"")
        private val pluginManagerImports = mapOf(
            "CollisionManager" to "import com.pandulapeter.kubriko.collision.CollisionManager",
            "KeyboardInputManager" to "import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager",
        )

        /**
         * [text] (a `Game.kt`) without the plugin Managers it creates (and their imports), which `KubrikoWiring` creates
         * when the game uses them, or null if it creates them in a way that doesn't look like the template's.
         */
        internal fun withoutPluginManagers(text: String): String? {
            val withoutManagers = text.lines().filterNot { line -> pluginManagerImports.keys.any { line.trim() == "$it.newInstance()," } }
            val withoutImports = withoutManagers.filterNot { line ->
                pluginManagerImports.any { (name, import) -> line.trim() == import && withoutManagers.none { name in it && !it.startsWith("import ") } }
            }
            return withoutImports.joinToString("\n").takeIf(::createsNoPluginManagers)
        }

        internal fun createsNoPluginManagers(text: String) = pluginManagerImports.keys.none { "$it.newInstance(" in text }

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

        internal fun setsCaches(text: String) = cacheProperties.none { text.lacksProperty(it) }

        /**
         * [text] (a `gradle.properties`) with the caches it doesn't set turned on.
         */
        internal fun withCaches(text: String) = (text.lines().dropLastWhile { it.isBlank() } + cacheProperties.filter { text.lacksProperty(it) }.map { "$it=true" })
            .joinToString("\n", postfix = "\n")

        private fun String.lacksProperty(key: String) = lines().none { line -> line.trimStart().startsWith(key) && line.trimStart().removePrefix(key).trimStart().let { it.startsWith("=") || it.startsWith(":") } }

        internal fun turnsLoggingOn(text: String) = LOGGING_SWITCH in text

        internal fun overridesFirstScene(text: String) = FIRST_SCENE_SWITCH in text

        /**
         * [text] (a desktop `Main.kt`) also setting the Scene the game starts with from the play session's system property,
         * right after it turns the Log Blocks on ([withLoggingTurnedOn], made first if it doesn't yet), or null if it can't be.
         */
        internal fun withFirstSceneOverride(text: String, wiringImport: String): String? {
            val withLogging = if (turnsLoggingOn(text)) text else withLoggingTurnedOn(text, wiringImport) ?: return null
            if (overridesFirstScene(withLogging)) return withLogging
            val lines = withLogging.lines().toMutableList()
            val logging = lines.indexOfFirst { LOGGING_SWITCH in it }.takeIf { lines.count { line -> LOGGING_SWITCH in line } == 1 } ?: return null
            val indent = lines[logging].takeWhile { it == ' ' }
            lines.add(logging + 1, "$indent$FIRST_SCENE_SWITCH = System.getProperty(\"${ManagerTypeCodeGenerator.FIRST_SCENE_PROPERTY}\")")
            return lines.joinToString("\n")
        }

        /**
         * [text] (a desktop `Main.kt`) turning the Log Blocks on from the play session's system property before the application
         * starts, or null if its `main` isn't the template's `application` expression at the end of the file.
         */
        internal fun withLoggingTurnedOn(text: String, wiringImport: String): String? {
            val lines = text.lines()
            val main = lines.indexOf(APPLICATION_MAIN).takeIf { it >= 0 && lines.count { line -> line == APPLICATION_MAIN } == 1 } ?: return null
            val end = lines.indexOfLast { it.isNotBlank() }
            if (lines[end] != "}") return null
            val body = lines.subList(main + 1, end + 1).map { if (it.isEmpty()) it else "    $it" }
            val changed = lines.subList(0, main) +
                listOf("fun main() {", "    $LOGGING_SWITCH = System.getProperty(\"${ManagerTypeCodeGenerator.LOGGING_PROPERTY}\").toBoolean()", "    application {") +
                body + "}" + lines.subList(end + 1, lines.size)
            return if (wiringImport in changed) changed.joinToString("\n") else withImport(changed, wiringImport)
        }

        /**
         * The `SceneLoader.kt` that templates before version 3 created in [packageName].
         */
        internal fun templateSceneLoader(packageName: String) = TemplateRenderer.renderContent(TemplateFiles.readResource(SCENE_LOADER_TEMPLATE), mapOf("PACKAGE" to packageName))

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
