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

import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import com.pandulapeter.kubrikoCreator.project.OwnWrites
import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * Reads and writes the block-built Actor types and Manager types of a project, keeping their generated Kotlin in sync.
 *
 * Definitions live in `.kubriko/actors` and `.kubriko/managers`, generated classes in the `generated` package of the
 * project's source folder. Deleting `.kubriko` leaves the generated classes in place, so the project keeps building.
 */
internal class BlockTypeStore private constructor(
    private val rootDirectory: File,
    private val settings: CreatorProjectSettings,
    private val ownWrites: OwnWrites,
) : BlockTypeRepository {
    /**
     * Where the generated classes are written, relative to the project root.
     */
    val generatedPath get() = "${settings.sourceDirectory}/${settings.packageName.replace('.', '/')}/${ActorTypeCodeGenerator.GENERATED_PACKAGE}"
    private val generatedDirectory get() = File(rootDirectory, generatedPath)

    /**
     * The definition files the last [load] couldn't read, which [save] and [delete] leave alone.
     */
    private var unreadableFiles = emptySet<File>()

    /**
     * The definition files of each kind by name, as the last [load] found them and [save] and [delete] changed them since,
     * so that a write needn't walk the definitions folder. Until the first [load] ([isIndexed]), writes walk it instead.
     */
    private val filesByName = HashMap<BlockTypeKind, HashMap<String, MutableSet<File>>>()
    private var isIndexed = false

    /**
     * Synchronized with [save] and [delete], which a write batch may run on another thread while a reload loads.
     */
    @Synchronized
    override fun load(): LoadedBlockTypes {
        val readable = ArrayList<Pair<String, BlockTypeDefinition>>()
        val unreadable = ArrayList<UnreadableBlockTypeFile>()
        filesByName.clear()
        BlockTypeKind.entries.forEach { kind ->
            val index = filesByName.getOrPut(kind) { HashMap() }
            definitionFiles(kind).forEach { file ->
                index.getOrPut(file.nameWithoutExtension) { HashSet() } += file
                val path = file.relativeTo(rootDirectory).invariantSeparatorsPath
                val definition = try {
                    decode(kind, file.readText()).withFolder(folderOf(kind, file))
                } catch (exception: Exception) {
                    unreadable += UnreadableBlockTypeFile(path, BlockTypeFileProblem.Unparsable(reasonOf(exception)))
                    return@forEach
                }
                when {
                    definition.name != file.nameWithoutExtension -> BlockTypeFileProblem.NameMismatch(definition.name)
                    !BlockTypeNaming.isWellFormed(definition.name) -> BlockTypeFileProblem.InvalidName(definition.name)
                    BlockTypeNaming.isReserved(definition.name, kind) -> BlockTypeFileProblem.ReservedName(definition.name)
                    else -> invalidPropertyNameOf(definition)?.let(BlockTypeFileProblem::InvalidPropertyName)
                }?.let { unreadable += UnreadableBlockTypeFile(path, it) } ?: run { readable += path to definition }
            }
        }
        val pathsByName = readable.groupBy({ it.second.name.lowercase() }, { it.first })
        val types = readable.mapNotNull { (path, definition) ->
            val paths = pathsByName.getValue(definition.name.lowercase())
            if (paths.size == 1) {
                definition
            } else {
                unreadable += UnreadableBlockTypeFile(path, BlockTypeFileProblem.DuplicateName((paths - path).sorted()))
                null
            }
        }
        unreadableFiles = unreadable.mapTo(HashSet()) { File(rootDirectory, it.path) }
        isIndexed = true
        return LoadedBlockTypes(types.sortedBy { it.name }, unreadable.sortedBy { it.path })
    }

    /**
     * The first of the type's own Properties whose name can't be one among the others (a name listed twice counts as
     * taken), or null. Inherited Properties are left to the commands, since a child may repeat one to change its value.
     */
    private fun invalidPropertyNameOf(definition: BlockTypeDefinition): String? {
        val names = definition.properties.map { it.name }
        return names.firstOrNull { name -> PropertyNaming.validate(name, names - name) != null }
    }

    private fun decode(kind: BlockTypeKind, text: String): BlockTypeDefinition = when (kind) {
        BlockTypeKind.ACTOR -> json.decodeFromString(ActorTypeDefinition.serializer(), text)
        BlockTypeKind.MANAGER -> json.decodeFromString(ManagerTypeDefinition.serializer(), text)
    }

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
     * Writes [definition], without regenerating any code ([regenerate] does that, once for many changes), and deletes the
     * other files of its name and kind that the last [load] found or this store wrote since (those of a type moved to
     * another folder). A file of the same name copied into another folder after that load is left alone, and the next
     * load reports both as the same name held twice.
     */
    @Synchronized
    override fun save(definition: BlockTypeDefinition) {
        val text = when (definition) {
            is ActorTypeDefinition -> json.encodeToString(ActorTypeDefinition.serializer(), definition)
            is ManagerTypeDefinition -> json.encodeToString(ManagerTypeDefinition.serializer(), definition)
        }
        val file = File(rootDirectory, definitionPath(definition))
        if (file in unreadableFiles) throw IOException("${file.path} couldn't be read, so it isn't written over.")
        deleteDefinitionFiles(definition.kind, definition.name, except = file)
        file.parentFile.mkdirs()
        file.writeTextAtomically(text + "\n")
        ownWrites.record(file)
        if (isIndexed) filesByName.getValue(definition.kind).getOrPut(definition.name) { HashSet() } += file
    }

    @Synchronized
    override fun delete(name: String) {
        BlockTypeKind.entries.forEach { kind -> deleteDefinitionFiles(kind, name) }
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

    /**
     * What the last [regenerate] generated from, and what it found, so that regenerating from the same types and Scene
     * music (after a Scene change, for example) does nothing.
     */
    private var lastGeneration: Pair<List<BlockTypeDefinition>, Set<String>>? = null

    override fun regenerate(definitions: List<BlockTypeDefinition>, overwriting: Set<String>): RegeneratedCode {
        val sceneMusic = sceneMusic()
        val typeNames = definitions.mapTo(HashSet()) { it.name }
        if (overwriting.isEmpty() && lastGeneration == definitions to sceneMusic && generatedFilesAreAsLeft()) return found(typeNames, removeLeftovers(typeNames))
        lastGeneration = null
        val actorTypes = definitions.filterIsInstance<ActorTypeDefinition>()
        val family = ActorTypeFamily(actorTypes)
        val actorTypeNames = actorTypes.map { it.name }
        val managerTypes = definitions.filterIsInstance<ManagerTypeDefinition>()
        val pluginManagers = PluginManagers.of(definitions, sceneMusic)
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
        val removedLeftovers = removeLeftovers(typeNames)
        lastGeneration = definitions to sceneMusic
        return found(typeNames, removedLeftovers)
    }

    private fun found(typeNames: Set<String>, removedLeftovers: List<String> = emptyList()) = RegeneratedCode(
        changedByHand = changedByHand.filterTo(HashSet()) { it in typeNames },
        removedLeftovers = removedLeftovers,
        leftoversChangedByHand = leftoversChangedByHand,
    )

    /**
     * The generated files the last [removeLeftovers] kept because they were changed by hand, by path from the project root.
     */
    private var leftoversChangedByHand = emptySet<String>()

    /**
     * Deletes the generated files that belong to none of [typeNames] and aren't one of the files every project has,
     * unless they were changed by hand, and returns their paths from the project root. Names are compared ignoring case,
     * since on most file systems a file differing only in case is the type's own.
     */
    private fun removeLeftovers(typeNames: Set<String>): List<String> {
        val currentNames = (typeNames + FIXED_FILE_NAMES).mapTo(HashSet()) { it.lowercase() }
        val removed = ArrayList<String>()
        val keptChangedByHand = HashSet<String>()
        generatedDirectory.listFiles().orEmpty().filter { it.isFile && it.extension == "kt" && it.nameWithoutExtension.lowercase() !in currentNames }.forEach { file ->
            val path = file.relativeTo(rootDirectory).invariantSeparatorsPath
            when {
                !GeneratedFile.isUnchanged(file.readText()) -> keptChangedByHand += path
                file.delete() -> removed += path
            }
            generatedTexts.remove(file.nameWithoutExtension)
            changedByHand.remove(file.nameWithoutExtension)
        }
        leftoversChangedByHand = keptChangedByHand
        return removed.sorted()
    }

    /**
     * Whether every generated file this store knows is still as it left it.
     */
    private fun generatedFilesAreAsLeft() = generatedTexts.all { (name, known) ->
        val file = generatedFile(name)
        known.length == file.length() && known.lastModified == file.lastModified()
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
            if (current?.replace("\r\n", "\n") != text) file.writeTextAtomically(text)
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

    private fun definitionFilesNamed(kind: BlockTypeKind, name: String): Set<File> = if (isIndexed) {
        filesByName.getValue(kind)[name].orEmpty()
    } else {
        definitionFiles(kind).filterTo(HashSet()) { it.nameWithoutExtension == name }
    }

    /**
     * Deletes the readable definition files of [name] and [kind] other than [except], keeping the index in step.
     */
    private fun deleteDefinitionFiles(kind: BlockTypeKind, name: String, except: File? = null) {
        val deleted = definitionFilesNamed(kind, name).filter { it != except && it !in unreadableFiles }.onEach { file ->
            file.delete()
            ownWrites.record(file)
        }
        if (isIndexed && deleted.isNotEmpty()) {
            val index = filesByName.getValue(kind)
            index[name]?.let { files ->
                files.removeAll { it in deleted && !it.exists() }
                if (files.isEmpty()) index.remove(name)
            }
        }
    }

    private fun folderOf(kind: BlockTypeKind, file: File) = file.parentFile.relativeTo(definitionsDirectory(kind)).invariantSeparatorsPath

    private fun generatedFile(name: String) = File(generatedDirectory, "$name.kt")

    private fun reasonOf(exception: Exception) = exception.message?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim() ?: exception::class.simpleName.orEmpty()

    companion object {
        const val CREATOR_DIRECTORY = ".kubriko"

        /**
         * The folder in [CREATOR_DIRECTORY] that holds the definitions of types of [kind].
         */
        fun definitionsFolder(kind: BlockTypeKind) = when (kind) {
            BlockTypeKind.ACTOR -> "actors"
            BlockTypeKind.MANAGER -> "managers"
        }

        /**
         * Where [definition] is saved, relative to the project's folder.
         */
        fun definitionPath(definition: BlockTypeDefinition) = "$CREATOR_DIRECTORY/${definitionsFolder(definition.kind)}/" + "${definition.folder}/${definition.name}.json".removePrefix("/")
        const val SETTINGS_FILE = "$CREATOR_DIRECTORY/project.json"

        /**
         * The generated files every project has, whatever its types.
         */
        private val FIXED_FILE_NAMES = setOf(
            ActorTypeCodeGenerator.REGISTRY_NAME,
            ScenesCodeGenerator.SCENES_NAME,
            SceneSettingsCodeGenerator.SETTINGS_NAME,
            ManagerTypeCodeGenerator.WIRING_NAME,
            AudioCodeGenerator.AUDIO_NAME,
        )

        private val json = Json {
            prettyPrint = true
            encodeDefaults = true
            explicitNulls = false
            ignoreUnknownKeys = true
        }

        /**
         * The store of the project in [rootDirectory], or null if the project hasn't opted in to block-built Actor types.
         */
        fun open(rootDirectory: File, ownWrites: OwnWrites = OwnWrites()) = File(rootDirectory, SETTINGS_FILE).takeIf { it.isFile }?.let { settingsFile ->
            BlockTypeStore(rootDirectory, json.decodeFromString(CreatorProjectSettings.serializer(), settingsFile.readText()), ownWrites)
        }
    }
}
