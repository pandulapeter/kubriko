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
 * still the template's. Version 4 leaves the plugins' Managers to `KubrikoWiring`, which creates only those the game uses,
 * so that the project only depends on the plugins it uses: `Game.kt` stops creating them. Version 5 lets play sessions turn
 * the Log Blocks on, which released games leave off: the desktop `Main.kt` sets `KubrikoWiring.isLoggingEnabled` from a system
 * property before the application starts. Version 6 turns Gradle's configuration cache and build cache on in
 * `gradle.properties` (a property set already, to any value, is kept; a missing file is created), so that Kubriko Creator's builds skip configuring
 * the project when nothing changed. Version 7 lets Kubriko Creator play the game from any Scene: the desktop `Main.kt` also sets
 * `KubrikoWiring.firstSceneOverride` from a system property. A file is only changed when it
 * still looks like the template's (one `Kubriko.newInstance` with a one-line `SceneLoader` in it, one-line plugin
 * Managers, one `createSerializationManager`); a file that already does what the version needs counts as done, whoever
 * changed it, and so does a `SceneLoader.kt` changed by hand, which is left alone.
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
        val isGameUpgraded = propose("$packagePath/$GAME_FILE", ::isGameUpToDate) { upgradedGame(it, wiringImport) }
        propose("$packagePath/$ENTRY_POINT_FILE", { ManagerTypeCodeGenerator.WIRING_NAME in it }) { withManagersHandedToTheSceneEditor(it, wiringImport) }
        val desktopPackagePath = "${sourceDirectory.replace("commonMain", "desktopMain")}/${packageName.replace('.', '/')}"
        propose("$desktopPackagePath/$MAIN_FILE", { turnsLoggingOn(it) && overridesFirstScene(it) }) { withFirstSceneOverride(it, wiringImport) }
        val properties = read(GRADLE_PROPERTIES_FILE).orEmpty()
        if (!setsCaches(properties)) changes += ProposedFileChange(GRADLE_PROPERTIES_FILE, properties, withCaches(properties))
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
                if ((if (file.isFile) file.readText() else "") == change.before) {
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
        const val CURRENT_VERSION = 7
        private const val GRADLE_PROPERTIES_FILE = "gradle.properties"
        private val cacheProperties = listOf("org.gradle.configuration-cache", "org.gradle.caching")
        private const val GAME_FILE = "Game.kt"
        private const val MAIN_FILE = "Main.kt"
        private const val APPLICATION_MAIN = "fun main() = application {"
        private const val LOGGING_SWITCH = "${ManagerTypeCodeGenerator.WIRING_NAME}.${ManagerTypeCodeGenerator.LOGGING_FLAG}"
        private const val FIRST_SCENE_SWITCH = "${ManagerTypeCodeGenerator.WIRING_NAME}.${ManagerTypeCodeGenerator.FIRST_SCENE_FLAG}"
        private const val ENTRY_POINT_FILE = "GameEntryPoint.kt"
        private const val SCENE_LOADER_FILE = "SceneLoader.kt"
        private const val SCENE_LOADER_TEMPLATE = "upgrades/SceneLoader-2.kt.template"
        private val json = Json { prettyPrint = true }
        private val scenePathPattern = Regex("\"${ScenesCodeGenerator.SCENE_DIRECTORY}/([^\"/]+)\\.json\"")
        private val pluginManagerImports = mapOf(
            "CollisionManager" to "import com.pandulapeter.kubriko.collision.CollisionManager",
            "KeyboardInputManager" to "import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager",
        )

        /**
         * Whether [text] (a `Game.kt`) is what the current template version needs.
         */
        internal fun isGameUpToDate(text: String) = loadsScenesThroughWiring(text) && createsNoPluginManagers(text)

        /**
         * [text] (a `Game.kt`) brought up to the current template version, or null if it doesn't look like the template's.
         */
        internal fun upgradedGame(text: String, wiringImport: String): String? {
            val withScenes = if (loadsScenesThroughWiring(text)) text else withScenesLoadedByWiring(text, wiringImport) ?: return null
            return withoutPluginManagers(withScenes)
        }

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

        private fun createsNoPluginManagers(text: String) = pluginManagerImports.keys.none { "$it.newInstance(" in text }

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

        private fun setsCaches(text: String) = cacheProperties.none { text.lacksProperty(it) }

        /**
         * [text] (a `gradle.properties`) with the caches it doesn't set turned on.
         */
        internal fun withCaches(text: String) = (text.lines().dropLastWhile { it.isBlank() } + cacheProperties.filter { text.lacksProperty(it) }.map { "$it=true" })
            .joinToString("\n", postfix = "\n")

        private fun String.lacksProperty(key: String) = lines().none { line -> line.trimStart().startsWith(key) && line.trimStart().removePrefix(key).trimStart().let { it.startsWith("=") || it.startsWith(":") } }

        private fun turnsLoggingOn(text: String) = LOGGING_SWITCH in text

        private fun overridesFirstScene(text: String) = FIRST_SCENE_SWITCH in text

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
