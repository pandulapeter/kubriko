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
        definitionFiles(BlockTypeKind.ACTOR).map { json.decodeFromString(ActorTypeDefinition.serializer(), it.readText()) } +
            definitionFiles(BlockTypeKind.MANAGER).map { json.decodeFromString(ManagerTypeDefinition.serializer(), it.readText()) }
        ).sortedBy { it.name }

    /**
     * Writes [definition] and regenerates the code of every type, since types refer to each other (in collision Events,
     * and through the Properties of Manager types).
     */
    override fun save(definition: BlockTypeDefinition): Set<String> {
        definitionsDirectory(definition.kind).mkdirs()
        val text = when (definition) {
            is ActorTypeDefinition -> json.encodeToString(ActorTypeDefinition.serializer(), definition)
            is ManagerTypeDefinition -> json.encodeToString(ManagerTypeDefinition.serializer(), definition)
        }
        definitionFile(definition.kind, definition.name).writeText(text + "\n")
        return regenerateAll()
    }

    override fun delete(name: String): Set<String> {
        BlockTypeKind.entries.forEach { definitionFile(it, name).delete() }
        generatedFile(name).delete()
        return regenerateAll()
    }

    override fun regenerateAll(overwriting: Set<String>): Set<String> {
        val definitions = load()
        val actorTypeNames = definitions.filterIsInstance<ActorTypeDefinition>().map { it.name }
        val managerTypes = definitions.filterIsInstance<ManagerTypeDefinition>()
        val pluginManagers = PluginManagers.of(definitions, sceneMusic())
        generatedDirectory.mkdirs()
        val changedByHand = definitions.mapNotNull { definition ->
            val file = generatedFile(definition.name)
            if (definition.name !in overwriting && file.isFile && !GeneratedFile.isUnchanged(file.readText())) {
                definition.name
            } else {
                file.writeIfChanged(
                    when (definition) {
                        is ActorTypeDefinition -> ActorTypeCodeGenerator.generateActorType(
                            definition = definition,
                            packageName = settings.packageName,
                            knownActorTypeNames = actorTypeNames,
                            managerTypes = managerTypes,
                            collidableActorTypeNames = pluginManagers.collidableActorTypeNames,
                        )
                        is ManagerTypeDefinition -> ManagerTypeCodeGenerator.generateManagerType(definition, settings.packageName, actorTypeNames, managerTypes)
                    },
                )
                null
            }
        }.toSet()
        writeUnlessChangedByHand(ActorTypeCodeGenerator.REGISTRY_NAME, ActorTypeCodeGenerator.generateRegistry(actorTypeNames, settings.packageName))
        writeUnlessChangedByHand(ScenesCodeGenerator.SCENES_NAME, ScenesCodeGenerator.generate(settings.packageName))
        writeUnlessChangedByHand(SceneSettingsCodeGenerator.SETTINGS_NAME, SceneSettingsCodeGenerator.generate(settings.packageName))
        writeUnlessChangedByHand(ManagerTypeCodeGenerator.WIRING_NAME, ManagerTypeCodeGenerator.generateWiring(managerTypes.map { it.name }, settings.packageName, pluginManagers))
        writeUnlessChangedByHand(
            AudioCodeGenerator.AUDIO_NAME,
            if (pluginManagers.usesAudio) AudioCodeGenerator.generate(settings.packageName, pluginManagers.sounds, pluginManagers.music) else null,
        )
        return changedByHand
    }

    /**
     * Writes the generated file [name], or deletes it if [text] is null, unless it was changed by hand.
     */
    private fun writeUnlessChangedByHand(name: String, text: String?) {
        val file = generatedFile(name)
        if (file.isFile && !GeneratedFile.isUnchanged(file.readText())) return
        if (text == null) file.delete() else file.writeIfChanged(text)
    }

    /**
     * Leaves a file with the same content alone, so that regenerating doesn't touch what didn't change.
     */
    private fun File.writeIfChanged(text: String) {
        if (!isFile || readText() != text) writeText(text)
    }

    /**
     * The music of every Scene the game can load (those next to the source folder, in `composeResources`) that has some.
     */
    private fun sceneMusic() = File(File(rootDirectory, settings.sourceDirectory).parentFile, "composeResources/${ScenesCodeGenerator.SCENE_DIRECTORY}")
        .listFiles { file -> file.extension == "json" }
        .orEmpty()
        .mapNotNullTo(mutableSetOf()) { SceneSettingsState.musicOf(it.readText()) }

    private fun definitionsDirectory(kind: BlockTypeKind) = File(rootDirectory, "$CREATOR_DIRECTORY/${definitionsFolder(kind)}")

    private fun definitionFiles(kind: BlockTypeKind) = definitionsDirectory(kind).listFiles { file -> file.extension == "json" }.orEmpty().toList()

    private fun definitionFile(kind: BlockTypeKind, name: String) = File(definitionsDirectory(kind), "$name.json")

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
