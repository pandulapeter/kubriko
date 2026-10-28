/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.project.OwnWrites
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * The Scene files of a project, by name: the file's path in the Scenes folder without `.json` (`levels/1-1`), since
 * Scenes can be in folders. Calls that write block, so callers run them off the main thread. Each Scene is kept parsed
 * in memory while its file has the length and modification time it was read or written with, so that commands check
 * and change a large Scene without reading it again; a file changed elsewhere is read again the next time it is asked for.
 *
 * @param sceneResources The Scene files the last scan of the project found. Scenes created since then are found in the
 *   folder new Scenes go to.
 */
internal class SceneStore(
    private val rootDirectory: File,
    private val ownWrites: OwnWrites = OwnWrites(),
    private val sceneResources: () -> List<ProjectResource>,
) {
    /**
     * Where new Scenes go: next to the existing ones, or where the project templates keep them.
     */
    private val sceneDirectory get() = sceneResources().firstNotNullOfOrNull { rootOf(it.file) } ?: File(rootDirectory, DEFAULT_SCENE_DIRECTORY)

    fun names() = (sceneResources().map { it.file } + sceneDirectory.walkTopDown().filter { it.extension == EXTENSION }.toList())
        .filter { it.isFile }
        .map(::nameOf)
        .distinct()
        .sorted()

    /**
     * Every folder of the Scenes folder, as paths (`levels/forest`), empty ones too.
     */
    fun folders(): Set<String> = sceneDirectory.walkTopDown()
        .filter { it.isDirectory && it != sceneDirectory }
        .mapTo(sortedSetOf()) { it.relativeTo(sceneDirectory).invariantSeparatorsPath }

    @EditorMutation
    fun createFolder(path: String) {
        File(sceneDirectory, path).mkdirs()
    }

    /**
     * Deletes the folder at [path] if no Scene is in it any more.
     */
    @EditorMutation
    fun deleteFolder(path: String) {
        val folder = File(sceneDirectory, path)
        if (folder.walkTopDown().none { it.isFile }) folder.deleteRecursively()
    }

    /**
     * Moves the Scene [oldName] to [newName], into another folder if its path says so.
     */
    @EditorMutation
    fun move(oldName: String, newName: String) {
        val source = file(oldName) ?: return
        val target = File(sceneDirectory, "$newName.$EXTENSION")
        target.parentFile.mkdirs()
        source.renameTo(target)
        ownWrites.record(source)
        ownWrites.record(target)
    }

    /**
     * Where the Scene [name] is, or would be written to.
     */
    fun fileOf(name: String) = file(name) ?: File(sceneDirectory, "$name.$EXTENSION")

    fun exists(name: String) = file(name) != null

    fun read(name: String) = cached(name)?.text

    /**
     * The Actors of the Scene [name], or null if it doesn't exist or isn't a Scene file.
     */
    fun actors(name: String) = cached(name)?.actors

    /**
     * Whether the Scene [name] exists, and Kubriko can read every Actor in it.
     */
    fun isValid(name: String) = cached(name)?.isValid == true

    @EditorMutation
    fun write(name: String, content: String) = write(name, content, actors = null)

    /**
     * Writes [actors] as the Scene [name], keeping them as they are for the next command instead of parsing them again.
     */
    @EditorMutation
    fun write(name: String, actors: List<SceneActorEntry>) = write(name, SceneDocument.format(actors), actors)

    private fun write(name: String, content: String, actors: List<SceneActorEntry>?) {
        val file = file(name) ?: File(sceneDirectory, "$name.$EXTENSION")
        file.parentFile.mkdirs()
        file.writeText(content)
        ownWrites.record(file)
        cache[file] = CachedScene(content, file.length(), file.lastModified(), actors)
    }

    private val cache = ConcurrentHashMap<File, CachedScene>()

    private fun cached(name: String): CachedScene? {
        val file = file(name) ?: return null
        val length = file.length()
        val lastModified = file.lastModified()
        return cache[file]?.takeIf { it.length == length && it.lastModified == lastModified }
            ?: CachedScene(file.readText(), length, lastModified).also { cache[file] = it }
    }

    /**
     * A Scene file's [text] as it was read or written, parsed once, when first needed (unless [knownActors] are given).
     */
    private class CachedScene(val text: String, val length: Long, val lastModified: Long, knownActors: List<SceneActorEntry>? = null) {
        val actors by lazy {
            knownActors ?: try {
                SceneDocument.parse(text)
            } catch (_: SerializationException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        val isValid by lazy { actors?.let(SceneDocument::isValid) == true }
    }

    @EditorMutation
    fun delete(name: String) {
        file(name)?.let { file ->
            file.delete()
            ownWrites.record(file)
        }
    }

    /**
     * Points the Actors of every Scene that are of the [oldTypeId] type to [newTypeId].
     *
     * @return The names of the Scenes that changed.
     */
    @EditorMutation
    fun renameTypeId(oldTypeId: String, newTypeId: String) = changeActors { actor -> if (actor.typeId == oldTypeId) actor.copy(typeId = newTypeId) else actor }

    /**
     * Changes the state of every Actor of the [typeId] type in every Scene with [change]. States that aren't JSON
     * objects are left alone.
     *
     * @return The names of the Scenes that changed.
     */
    @EditorMutation
    fun changeStates(typeId: String, change: (JsonObject) -> JsonObject) = changeActors { actor ->
        if (actor.typeId == typeId) SceneDocument.withStateChanged(actor.state, change)?.let { actor.copy(state = it) } ?: actor else actor
    }

    @EditorMutation
    private fun changeActors(change: (SceneActorEntry) -> SceneActorEntry) = names().filter { name ->
        val actors = actors(name) ?: return@filter false
        val changed = actors.map(change)
        if (changed == actors) return@filter false
        write(name, changed)
        true
    }

    private fun file(name: String) = sceneResources().firstOrNull { nameOf(it.file) == name && it.file.isFile }?.file
        ?: File(sceneDirectory, "$name.$EXTENSION").takeIf { it.isFile }

    companion object {

        /**
         * The name of the Scene in [file]: its path from the Scenes folder (`files/scenes` in `composeResources`) without
         * `.json`, or its file name for one outside it.
         */
        fun nameOf(file: File) = rootOf(file)?.let { file.relativeTo(it).invariantSeparatorsPath.removeSuffix(".$EXTENSION") } ?: file.nameWithoutExtension

        /**
         * The Scenes folder [file] is in, if any.
         */
        private fun rootOf(file: File) = generateSequence(file.parentFile) { it.parentFile }.firstOrNull { it.name == "scenes" && it.parentFile?.name == "files" }

        const val DEFAULT_SCENE_DIRECTORY = "src/commonMain/composeResources/files/scenes"
        private const val EXTENSION = "json"
    }
}
