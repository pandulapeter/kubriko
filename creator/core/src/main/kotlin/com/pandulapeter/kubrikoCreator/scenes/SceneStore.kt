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

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import com.pandulapeter.kubrikoCreator.project.OwnWrites
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * The Scene files of a project, by name: the file's path in the Scenes folder without `.json` (`levels/1-1`), since
 * Scenes can be in folders. Calls that write block, so callers run them off the main thread. Each Scene is kept parsed
 * in memory while its file has the length and modification time it was read or written with, so that commands check
 * and change a large Scene without reading it again; a file changed elsewhere is read again the next time it is asked for.
 * The names are known from the last [refresh] and Creator's own writes, so validating a command reads nothing. What
 * changes every Scene finds them on disk instead ([scanNames]), so that it doesn't miss one made outside Creator since.
 * Every change it makes is recorded for [takeChanges], so that what changed is known without each caller saying so.
 *
 * @param sceneResources The Scene files the last scan of the project found. Scenes created since then are found in the
 *   folder new Scenes go to.
 * @param homeDirectory The Scenes folder the game loads its Scenes from, when Kubriko Creator knows it (from the generated
 *   code's module), which new Scenes go to even when another module has Scenes, or none has any yet.
 * @param writeText Writes a Scene file, replaceable so that tests can make one fail.
 * @param readText Reads a Scene file, replaceable so that tests can make one fail.
 * @param beforeIndexing Runs between a [refresh]'s walk through the Scenes and its update of the names, so that tests can
 *   write meanwhile.
 */
public class SceneStore(
    private val rootDirectory: File,
    private val ownWrites: OwnWrites = OwnWrites(),
    private val writeText: (File, String) -> Unit = { file, text -> file.writeTextAtomically(text) },
    private val readText: (File) -> String = { file -> file.readText() },
    private val beforeIndexing: () -> Unit = {},
    private val homeDirectory: File? = null,
    private val sceneResources: () -> List<ProjectResource>,
) {
    /**
     * What each Scene is for, which moves and goes with it.
     */
    public val descriptions: SceneDescriptions = SceneDescriptions(rootDirectory, ownWrites)

    private val sceneDirectory get() = newSceneDirectoryOf(rootDirectory, sceneResources(), homeDirectory)

    private val indexLock = Any()
    private val refreshLock = Any()

    /**
     * The names of every Scene as of the last [refresh] and Creator's writes since, sorted; null before the first refresh.
     */
    @Volatile
    private var knownNames: List<String>? = null
    private val addedDuringWalk = HashSet<String>()
    private val removedDuringWalk = HashSet<String>()
    private val recordedChanges = ConcurrentLinkedQueue<SceneChange>()

    /**
     * The changes made since the last call, oldest first.
     */
    public fun takeChanges(): List<SceneChange> = buildList {
        while (true) add(recordedChanges.poll() ?: break)
    }

    /**
     * The names of every Scene, sorted, as of the last [refresh] and Creator's own writes since (before the first refresh,
     * as the files are now).
     */
    public fun names(): List<String> = knownNames ?: scanNames()

    /**
     * The names of every Scene as the files are now, which walks the Scenes folder, for what changes every Scene.
     */
    public fun scanNames(): List<String> = sceneFiles().map(::nameOf).distinct().sorted()

    private fun sceneFiles() = (sceneResources().map { it.file } + sceneDirectory.walkTopDown().filter { it.extension == EXTENSION }.toList())
        .filter { it.isFile }

    /**
     * Finds every Scene again for [names], and reads each whose file changed since it was last read, so that commands
     * validated afterwards read nothing. Blocks, so callers run it off the main thread.
     */
    public fun refresh(): Unit = synchronized(refreshLock) {
        synchronized(indexLock) {
            addedDuringWalk.clear()
            removedDuringWalk.clear()
        }
        val files = sceneFiles()
        files.forEach { cached(it) }
        val walked = files.map(::nameOf)
        beforeIndexing()
        synchronized(indexLock) {
            knownNames = (walked - removedDuringWalk + addedDuringWalk).distinct().sorted()
        }
    }

    private fun onWritten(name: String) = synchronized(indexLock) {
        removedDuringWalk -= name
        addedDuringWalk += name
        knownNames = knownNames?.let { names -> if (names.binarySearch(name) >= 0) names else (names + name).sorted() }
    }

    private fun onRemoved(name: String) = synchronized(indexLock) {
        addedDuringWalk -= name
        removedDuringWalk += name
        knownNames = knownNames?.let { names -> names - name }
    }

    /**
     * Every folder of the Scenes folder, as paths (`levels/forest`), empty ones too.
     */
    public fun folders(): Set<String> = foldersIn(sceneDirectory)

    @EditorMutation
    public fun createFolder(path: String) {
        if (File(sceneDirectory, path).mkdirs()) recordedChanges += SceneChange.FoldersChanged
    }

    /**
     * Deletes the folder at [path] if no Scene is in it any more.
     */
    @EditorMutation
    public fun deleteFolder(path: String) {
        val folder = File(sceneDirectory, path)
        if (folder.isDirectory && folder.walkTopDown().none { it.isFile } && folder.deleteRecursively()) recordedChanges += SceneChange.FoldersChanged
    }

    /**
     * Moves the Scene [oldName] to [newName], into another folder if its path says so, within the Scenes folder it is in
     * (in a project of several modules, each may have one).
     *
     * @return Whether it was moved: Windows refuses to move a file another program (the running game) has open.
     */
    @EditorMutation
    public fun move(oldName: String, newName: String): Boolean {
        val source = file(oldName) ?: return false
        val target = File(rootOf(source) ?: sceneDirectory, "$newName.$EXTENSION")
        val isMoved = try {
            target.parentFile.mkdirs()
            if (source.canonicalPath.equals(target.canonicalPath, ignoreCase = true)) {
                // Only the case changes, which Files.move takes for a move onto the same file where names ignore case.
                source.renameTo(target)
            } else {
                Files.move(source.toPath(), target.toPath())
                true
            }
        } catch (_: IOException) {
            false
        }
        if (!isMoved) return false
        cache.remove(source)
        ownWrites.record(source)
        ownWrites.record(target)
        onRemoved(oldName)
        onWritten(newName)
        recordedChanges += SceneChange.Moved(oldName, newName)
        descriptions.move(oldName, newName)
        return true
    }

    /**
     * Where the Scene [name] is, or would be written to.
     */
    public fun fileOf(name: String): File = file(name) ?: File(sceneDirectory, "$name.$EXTENSION")

    public fun exists(name: String): Boolean = knownNames?.let { names -> names.binarySearch(name) >= 0 } ?: (file(name) != null)

    public fun read(name: String): String? = cached(name)?.text

    /**
     * The Actors of the Scene [name], or null if it doesn't exist or isn't a Scene file.
     */
    public fun actors(name: String): List<SceneActorEntry>? = cached(name)?.actors

    /**
     * Whether the Scene [name] exists, and Kubriko can read every Actor in it.
     */
    public fun isValid(name: String): Boolean = cached(name)?.isValid == true

    /**
     * Why the Scene [name] can't be read, or null if it can (or doesn't exist).
     */
    public fun problemOf(name: String): SceneFileProblem? = cached(name)?.problem

    @EditorMutation
    public fun write(name: String, content: String) {
        recordedChanges += writeFile(name, content, actors = null)
    }

    /**
     * Writes [actors] as the Scene [name], keeping them as they are for the next command instead of parsing them again.
     */
    @EditorMutation
    public fun write(name: String, actors: List<SceneActorEntry>) {
        recordedChanges += writeFile(name, SceneDocument.format(actors), actors)
    }

    private fun writeFile(name: String, content: String, actors: List<SceneActorEntry>?): SceneChange.Written {
        val existing = file(name)
        val file = existing ?: File(sceneDirectory, "$name.$EXTENSION")
        file.parentFile.mkdirs()
        writeText(file, content)
        ownWrites.record(file)
        cache[file] = CachedScene(content, file.length(), file.lastModified(), actors)
        onWritten(name)
        return SceneChange.Written(name, isNew = existing == null)
    }

    private val cache = ConcurrentHashMap<File, CachedScene>()

    private fun cached(name: String): CachedScene? = file(name)?.let { cached(it) }

    private fun cached(file: File): CachedScene? {
        val length = file.length()
        val lastModified = file.lastModified()
        return cache[file]?.takeIf { it.length == length && it.lastModified == lastModified }
            ?: try {
                CachedScene(readText(file), length, lastModified).also { cache[file] = it }
            } catch (_: IOException) {
                null
            }
    }

    /**
     * A Scene file's [text] as it was read or written, parsed once, when first needed (unless [knownActors] are given).
     */
    private class CachedScene(val text: String, val length: Long, val lastModified: Long, knownActors: List<SceneActorEntry>? = null) {
        private val parsed by lazy {
            knownActors?.let { Result.success(it) } ?: try {
                Result.success(SceneDocument.parse(text))
            } catch (exception: SerializationException) {
                Result.failure(exception)
            } catch (exception: IllegalArgumentException) {
                Result.failure(exception)
            }
        }
        val actors get() = parsed.getOrNull()
        val problem by lazy {
            parsed.fold(
                onSuccess = { actors -> actors.firstOrNull { SceneDocument.stateObject(it.state) == null }?.let { SceneFileProblem.StateNotAnObject(it.id) } },
                onFailure = { SceneFileProblem.Unparsable(it.message?.lineSequence()?.first() ?: it.toString()) },
            )
        }
        val isValid by lazy { actors != null && problem == null }
    }

    @EditorMutation
    public fun delete(name: String) {
        if (deleteFile(name)) recordedChanges += SceneChange.Deleted(name)
        descriptions.set(name, null)
    }

    private fun deleteFile(name: String): Boolean {
        val file = file(name) ?: return false
        file.delete()
        cache.remove(file)
        ownWrites.record(file)
        onRemoved(name)
        return true
    }

    /**
     * Points the Actors of every Scene that are of the [oldTypeId] type to [newTypeId].
     *
     * @return The names of the Scenes that changed.
     */
    @EditorMutation
    public fun renameTypeId(oldTypeId: String, newTypeId: String): List<String> = changeActors { actor -> if (actor.typeId == oldTypeId) actor.copy(typeId = newTypeId) else actor }.keys.toList()

    /**
     * Changes the state of every Actor of the [typeIds] types in every Scene with [change], writing them all or none
     * ([writeAll]). States that aren't JSON objects are left alone.
     *
     * @return The states it replaced, by Actor ID, by the name of the Scene, for the Actors that changed.
     */
    @EditorMutation
    public fun changeStates(typeIds: Set<String>, change: (JsonObject) -> JsonObject): Map<String, Map<String, String>> = changeActors { actor ->
        if (actor.typeId in typeIds) SceneDocument.withStateChanged(actor.state, change)?.let { actor.copy(state = it) } ?: actor else actor
    }.mapValues { (_, changed) -> changed.associate { (before, _) -> before.id to before.state } }

    /**
     * Writes every Scene in [changes], or, when one can't be written, puts back those written before it and throws,
     * recording no change.
     */
    @EditorMutation
    internal fun writeAll(changes: Map<String, List<SceneActorEntry>>): Unit = writeFiles(changes.mapValues { (_, actors) -> SceneDocument.format(actors) to actors })

    /**
     * Writes each Scene in [contents] as the exact text given, all or none, as [writeAll] does: how a change to several
     * Scenes is undone byte for byte.
     */
    @EditorMutation
    public fun writeAllTexts(contents: Map<String, String>): Unit = writeFiles(contents.mapValues { (_, content) -> content to null })

    private fun writeFiles(changes: Map<String, Pair<String, List<SceneActorEntry>?>>) {
        val originals = changes.keys.associateWith { name -> cached(name)?.text }
        val written = ArrayList<SceneChange.Written>()
        try {
            changes.forEach { (name, change) -> written += writeFile(name, change.first, change.second) }
        } catch (exception: IOException) {
            written.forEach { change ->
                try {
                    val original = originals.getValue(change.name)
                    if (original != null) writeFile(change.name, original, actors = null) else deleteFile(change.name)
                } catch (restoreException: IOException) {
                    exception.addSuppressed(restoreException)
                }
            }
            throw exception
        }
        recordedChanges += written
    }

    /**
     * Changes the Actors of every Scene with [change], and returns each changed Actor as it was and as it became, by the
     * name of its Scene.
     */
    @EditorMutation
    private fun changeActors(change: (SceneActorEntry) -> SceneActorEntry): Map<String, List<Pair<SceneActorEntry, SceneActorEntry>>> {
        val changes = LinkedHashMap<String, List<SceneActorEntry>>()
        val changedActors = LinkedHashMap<String, List<Pair<SceneActorEntry, SceneActorEntry>>>()
        scanNames().forEach { name ->
            val actors = actors(name) ?: return@forEach
            val changed = actors.map(change)
            if (changed != actors) {
                changes[name] = changed
                changedActors[name] = actors.zip(changed).filter { (before, after) -> before != after }
            }
        }
        writeAll(changes)
        return changedActors
    }

    private fun file(name: String) = sceneResources().firstOrNull { nameOf(it.file) == name && it.file.isFile }?.file
        ?: File(sceneDirectory, "$name.$EXTENSION").takeIf { it.isFile }

    public companion object {

        /**
         * The name of the Scene in [file]: its path from the Scenes folder (`files/scenes` in `composeResources`) without
         * `.json`, or its file name for one outside it.
         */
        public fun nameOf(file: File): String = rootOf(file)?.let { file.relativeTo(it).invariantSeparatorsPath.removeSuffix(".$EXTENSION") } ?: file.nameWithoutExtension

        /**
         * The Scenes folder [file] is in, if any.
         */
        private fun rootOf(file: File) = generateSequence(file.parentFile) { it.parentFile }.firstOrNull { it.name == "scenes" && it.parentFile?.name == "files" }

        /**
         * The folder new Scenes and Scene folders of the project in [rootDirectory] go to, which has the Scene files
         * [sceneResources]: the [homeDirectory] the game loads them from if known, or else next to the existing Scenes, or
         * where the project templates keep them.
         */
        public fun newSceneDirectoryOf(rootDirectory: File, sceneResources: List<ProjectResource>, homeDirectory: File? = null): File =
            homeDirectory ?: sceneResources.firstNotNullOfOrNull { rootOf(it.file) } ?: File(rootDirectory, DEFAULT_SCENE_DIRECTORY)

        /**
         * Every folder of the Scenes folder [directory], as paths (`levels/forest`), empty ones too.
         */
        public fun foldersIn(directory: File): Set<String> = directory.walkTopDown()
            .filter { it.isDirectory && it != directory }
            .mapTo(sortedSetOf()) { it.relativeTo(directory).invariantSeparatorsPath }

        public const val DEFAULT_SCENE_DIRECTORY: String = "src/commonMain/composeResources/files/scenes"
        private const val EXTENSION = "json"
    }
}
