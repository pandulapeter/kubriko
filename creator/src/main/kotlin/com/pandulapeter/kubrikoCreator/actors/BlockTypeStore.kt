/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Reads and writes the block-built Actor types and Manager types of a project, keeping their generated Kotlin in sync.
 *
 * Definitions live in `.kubriko/actors` and `.kubriko/managers`, generated classes in the `generated` package of the
 * project's source folder. Deleting `.kubriko` leaves the generated classes in place, so the project keeps building.
 */
internal class BlockTypeStore private constructor(
    private val rootDirectory: File,
    private val settings: CreatorProjectSettings,
) : BlockTypeRepository {
    /**
     * Where the generated classes are written, relative to the project root.
     */
    val generatedPath get() = "${settings.sourceDirectory}/${settings.packageName.replace('.', '/')}/${ActorTypeCodeGenerator.GENERATED_PACKAGE}"
    private val generatedDirectory get() = File(rootDirectory, generatedPath)

    override fun load(): List<BlockTypeDefinition> = (
        definitionFiles(BlockTypeKind.ACTOR).map { json.decodeFromString(ActorTypeDefinition.serializer(), it.readText()).withFolder(folderOf(BlockTypeKind.ACTOR, it)) } +
            definitionFiles(BlockTypeKind.MANAGER).map { json.decodeFromString(ManagerTypeDefinition.serializer(), it.readText()).withFolder(folderOf(BlockTypeKind.MANAGER, it)) }
        ).sortedBy { it.name }

    override fun folders(kind: BlockTypeKind): Set<String> = definitionsDirectory(kind).walkTopDown()
        .filter { it.isDirectory && it != definitionsDirectory(kind) }
        .mapTo(sortedSetOf()) { it.relativeTo(definitionsDirectory(kind)).invariantSeparatorsPath }

    override fun createFolder(kind: BlockTypeKind, path: String) {
        File(definitionsDirectory(kind), path).mkdirs()
    }

    override fun deleteFolder(kind: BlockTypeKind, path: String) {
        val folder = File(definitionsDirectory(kind), path)
        if (folder.walkTopDown().none { it.isFile }) folder.deleteRecursively()
    }

    /**
     * Writes [definition], without regenerating any code ([regenerate] does that, once for many changes).
     */
    override fun save(definition: BlockTypeDefinition) {
        val text = when (definition) {
            is ActorTypeDefinition -> json.encodeToString(ActorTypeDefinition.serializer(), definition)
            is ManagerTypeDefinition -> json.encodeToString(ManagerTypeDefinition.serializer(), definition)
        }
        val file = File(definitionsDirectory(definition.kind), "${definition.folder}/${definition.name}.json".removePrefix("/"))
        // A type moved to another folder leaves its old file behind otherwise.
        definitionFiles(definition.kind).filter { it.nameWithoutExtension == definition.name && it != file }.forEach(File::delete)
        file.parentFile.mkdirs()
        file.writeText(text + "\n")
    }

    override fun delete(name: String) {
        BlockTypeKind.entries.forEach { kind -> definitionFiles(kind).filter { it.nameWithoutExtension == name }.forEach(File::delete) }
        generatedFile(name).delete()
        generatedTexts.remove(name)
        changedByHand.remove(name)
    }

    /**
     * What each generated file held when Kubriko Creator last read or wrote it, by type name, with the length and
     * modification time it had then, so that a file whose new code is the same isn't read again unless it changed since.
     */
    private val generatedTexts = HashMap<String, KnownFile>()

    private class KnownFile(val text: String, val length: Long, val lastModified: Long)

    /**
     * The types whose generated code was changed by hand, as last found: one whose code didn't change since stays here.
     */
    private val changedByHand = HashSet<String>()

    override fun regenerate(definitions: List<BlockTypeDefinition>, overwriting: Set<String>): Set<String> {
        val actorTypes = definitions.filterIsInstance<ActorTypeDefinition>()
        val family = ActorTypeFamily(actorTypes)
        val actorTypeNames = actorTypes.map { it.name }
        val managerTypes = definitions.filterIsInstance<ManagerTypeDefinition>()
        val pluginManagers = PluginManagers.of(definitions, sceneMusic())
        generatedDirectory.mkdirs()
        definitions.forEach { definition ->
            writeUnlessChangedByHand(
                name = definition.name,
                isOverwriting = definition.name in overwriting,
                text = when (definition) {
                    is ActorTypeDefinition -> ActorTypeCodeGenerator.generateActorType(
                        definition = definition,
                        packageName = settings.packageName,
                        knownActorTypeNames = actorTypeNames,
                        managerTypes = managerTypes,
                        collidableActorTypeNames = pluginManagers.collidableActorTypeNames,
                        actorTypes = actorTypes,
                        family = family,
                    )
                    is ManagerTypeDefinition -> ManagerTypeCodeGenerator.generateManagerType(definition, settings.packageName, actorTypeNames, managerTypes)
                },
            )
        }
        writeUnlessChangedByHand(ActorTypeCodeGenerator.REGISTRY_NAME, ActorTypeCodeGenerator.generateRegistry(actorTypeNames, settings.packageName))
        writeUnlessChangedByHand(ScenesCodeGenerator.SCENES_NAME, ScenesCodeGenerator.generate(settings.packageName))
        writeUnlessChangedByHand(SceneSettingsCodeGenerator.SETTINGS_NAME, SceneSettingsCodeGenerator.generate(settings.packageName))
        writeUnlessChangedByHand(ManagerTypeCodeGenerator.WIRING_NAME, ManagerTypeCodeGenerator.generateWiring(managerTypes.map { it.name }, settings.packageName, pluginManagers))
        writeUnlessChangedByHand(
            AudioCodeGenerator.AUDIO_NAME,
            if (pluginManagers.usesAudio) AudioCodeGenerator.generate(settings.packageName, pluginManagers.sounds, pluginManagers.music) else null,
        )
        val typeNames = definitions.mapTo(HashSet()) { it.name }
        return changedByHand.filterTo(HashSet()) { it in typeNames }
    }

    /**
     * Writes the generated file [name], or deletes it if [text] is null, unless it was changed by hand (and isn't being
     * [isOverwriting]). A file that already holds [text] is left alone, without even reading it when this store wrote
     * or read that text last.
     */
    private fun writeUnlessChangedByHand(name: String, text: String?, isOverwriting: Boolean = false) {
        val file = generatedFile(name)
        val known = generatedTexts[name]
        if (!isOverwriting && known != null && known.text == text && known.length == file.length() && known.lastModified == file.lastModified()) return
        val current = if (file.isFile) file.readText() else null
        if (current != null && !isOverwriting && !GeneratedFile.isUnchanged(current)) {
            changedByHand += name
            generatedTexts.remove(name)
            return
        }
        changedByHand -= name
        if (text == null) {
            file.delete()
            generatedTexts.remove(name)
        } else {
            if (current != text) file.writeText(text)
            generatedTexts[name] = KnownFile(text, file.length(), file.lastModified())
        }
    }

    /**
     * The music of every Scene the game can load (those next to the source folder, in `composeResources`) that has some.
     */
    private fun sceneMusic(): Set<String> {
        val scenes = File(File(rootDirectory, settings.sourceDirectory).parentFile, "composeResources/${ScenesCodeGenerator.SCENE_DIRECTORY}")
            .walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .toSet()
        musicByScene.keys.retainAll(scenes)
        return scenes.mapNotNullTo(mutableSetOf()) { file ->
            val length = file.length()
            val lastModified = file.lastModified()
            val known = musicByScene[file]?.takeIf { it.length == length && it.lastModified == lastModified }
            (known ?: SceneMusic(length, lastModified, SceneSettingsState.musicOf(file.readText())).also { musicByScene[file] = it }).music
        }
    }

    /**
     * The music of each Scene file, kept by its length and modification time, since reading a large Scene costs more
     * than generating the code.
     */
    private val musicByScene = HashMap<File, SceneMusic>()

    private class SceneMusic(val length: Long, val lastModified: Long, val music: String?)

    private fun definitionsDirectory(kind: BlockTypeKind) = File(rootDirectory, "$CREATOR_DIRECTORY/${definitionsFolder(kind)}")

    private fun definitionFiles(kind: BlockTypeKind) = definitionsDirectory(kind).walkTopDown().filter { it.isFile && it.extension == "json" }.toList()

    private fun folderOf(kind: BlockTypeKind, file: File) = file.parentFile.relativeTo(definitionsDirectory(kind)).invariantSeparatorsPath

    private fun generatedFile(name: String) = File(generatedDirectory, "$name.kt")

    companion object {
        const val CREATOR_DIRECTORY = ".kubriko"

        /**
         * The folder in [CREATOR_DIRECTORY] that holds the definitions of types of [kind].
         */
        fun definitionsFolder(kind: BlockTypeKind) = when (kind) {
            BlockTypeKind.ACTOR -> "actors"
            BlockTypeKind.MANAGER -> "managers"
        }
        const val SETTINGS_FILE = "$CREATOR_DIRECTORY/project.json"

        private val json = Json {
            prettyPrint = true
            encodeDefaults = true
            explicitNulls = false
            ignoreUnknownKeys = true
        }

        /**
         * The store of the project in [rootDirectory], or null if the project hasn't opted in to block-built Actor types.
         */
        fun open(rootDirectory: File) = File(rootDirectory, SETTINGS_FILE).takeIf { it.isFile }?.let { settingsFile ->
            BlockTypeStore(rootDirectory, json.decodeFromString(CreatorProjectSettings.serializer(), settingsFile.readText()))
        }
    }
}
