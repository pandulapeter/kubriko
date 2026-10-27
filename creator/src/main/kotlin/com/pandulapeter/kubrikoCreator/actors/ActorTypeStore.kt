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

import kotlinx.serialization.json.Json
import java.io.File

/**
 * Reads and writes the block-built Actor types of a project, keeping their generated Kotlin in sync.
 *
 * Definitions live in `.kubriko/actors`, generated classes in the `generated` package of the project's source folder.
 * Deleting `.kubriko` leaves the generated classes in place, so the project keeps building.
 */
internal class ActorTypeStore private constructor(
    private val rootDirectory: File,
    private val settings: CreatorProjectSettings,
) : ActorTypeRepository {
    private val definitionsDirectory get() = File(rootDirectory, "$CREATOR_DIRECTORY/actors")
    private val generatedDirectory
        get() = File(rootDirectory, "${settings.sourceDirectory}/${settings.packageName.replace('.', '/')}/${ActorTypeCodeGenerator.GENERATED_PACKAGE}")

    override fun load(): List<ActorTypeDefinition> = definitionsDirectory.listFiles { file -> file.extension == "json" }
        .orEmpty()
        .map { json.decodeFromString(ActorTypeDefinition.serializer(), it.readText()) }
        .sortedBy { it.name }

    override fun save(definition: ActorTypeDefinition) {
        definitionsDirectory.mkdirs()
        definitionFile(definition.name).writeText(json.encodeToString(ActorTypeDefinition.serializer(), definition) + "\n")
        generatedDirectory.mkdirs()
        generatedFile(definition.name).writeText(ActorTypeCodeGenerator.generateActorType(definition, settings.packageName))
        writeRegistry()
    }

    override fun delete(name: String) {
        definitionFile(name).delete()
        generatedFile(name).delete()
        writeRegistry()
    }

    /**
     * Rewrites every generated file from the definitions, for example after a template was copied.
     */
    fun regenerateAll() = load().forEach(::save).also { writeRegistry() }

    private fun writeRegistry() {
        generatedDirectory.mkdirs()
        File(generatedDirectory, "${ActorTypeCodeGenerator.REGISTRY_NAME}.kt").writeText(
            ActorTypeCodeGenerator.generateRegistry(load().map { it.name }, settings.packageName),
        )
    }

    private fun definitionFile(name: String) = File(definitionsDirectory, "$name.json")

    private fun generatedFile(name: String) = File(generatedDirectory, "$name.kt")

    companion object {
        const val CREATOR_DIRECTORY = ".kubriko"
        private const val SETTINGS_FILE = "$CREATOR_DIRECTORY/project.json"

        private val json = Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

        /**
         * The store of the project in [rootDirectory], or null if the project hasn't opted in to block-built Actor types.
         */
        fun open(rootDirectory: File) = File(rootDirectory, SETTINGS_FILE).takeIf { it.isFile }?.let { settingsFile ->
            ActorTypeStore(rootDirectory, json.decodeFromString(CreatorProjectSettings.serializer(), settingsFile.readText()))
        }
    }
}
