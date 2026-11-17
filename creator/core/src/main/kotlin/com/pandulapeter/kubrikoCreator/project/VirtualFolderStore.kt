/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.moveWithoutReplacing
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * The folders of a kind of file whose sidebar folders are virtual, since Compose resources make accessors only of the
 * files right in their folder (`drawable`, `font`): each a directory of [directoryPath] (inside `.kubriko`), holding a
 * `<name>.json` file for each file in it, named after the file without its extension (such names are unique, as their
 * accessors are). A file without one is in no folder. That file holds what Kubriko Creator keeps about the file besides
 * its folder ([DEFAULT_TEXT] for nothing). Calls that change it block, so callers run them off the main thread; [state] is
 * read anywhere.
 *
 * @param S What [state] holds: the folders, and what a kind keeps besides ([stateOf]).
 */
public abstract class VirtualFolderStore<S : VirtualFolders>(
    private val rootDirectory: File,
    protected val ownWrites: OwnWrites,
    private val directoryPath: String,
    initialState: S,
) {
    protected val directory: File get() = File(rootDirectory, directoryPath)
    private val _state = MutableStateFlow(initialState)
    public val state: StateFlow<S> = _state.asStateFlow()

    @Volatile
    private var filesByName: Map<String, List<File>> = emptyMap()

    /**
     * What [state] holds for [folders] (as paths, empty ones too) and [folderOf] (the folder of each file in one, by its
     * name), read from [filesByName] (the files of each, in order of their paths).
     */
    protected abstract fun stateOf(folders: Set<String>, folderOf: Map<String, String>, filesByName: Map<String, List<File>>): S

    /**
     * Reads the folders again, returning the [state] it read. Blocks. A file with entries in several folders (two branches
     * moving it, merged) is in the first of them by path, and every change to it acts on all of them.
     */
    public fun refresh(): S = synchronized(this) {
        val root = directory
        val files = root.walkTopDown().filter { it != root }.sortedBy { it.invariantSeparatorsPath }.toList()
        filesByName = files.filter { it.isFile && it.extension == EXTENSION }.groupBy { it.nameWithoutExtension }
        val state = stateOf(
            folders = files.filter { it.isDirectory }.mapTo(sortedSetOf()) { it.relativeTo(root).invariantSeparatorsPath },
            folderOf = filesByName.mapValues { (_, named) -> named.first().parentFile.relativeTo(root).invariantSeparatorsPath },
            filesByName = filesByName,
        )
        _state.value = state
        state
    }

    @EditorMutation
    public fun createFolder(path: String) {
        File(directory, path).mkdirs()
        refresh()
    }

    /**
     * Deletes the folder at [path] if nothing is in it any more.
     */
    @EditorMutation
    public fun deleteFolder(path: String) {
        val folder = File(directory, path)
        if (folder.isDirectory && folder.walkTopDown().none { it.isFile } && folder.deleteRecursively()) refresh()
    }

    /**
     * Moves the folder at [path], with every folder and file in it, to [newPath].
     *
     * @return Whether everything moved; when something couldn't, what moved is moved back.
     */
    @EditorMutation
    public fun moveFolder(path: String, newPath: String): Boolean {
        val from = File(directory, path)
        val to = File(directory, newPath)
        val contents = from.walkTopDown().filter { it != from }.toList()
        to.mkdirs()
        contents.filter { it.isDirectory }.forEach { File(to, it.relativeTo(from).path).mkdirs() }
        val moved = ArrayList<Pair<File, File>>()
        val isMoved = contents.filter { it.isFile }.all { file ->
            val target = File(to, file.relativeTo(from).path)
            file.moveWithoutReplacing(target).also { if (it) moved += file to target }
        }
        if (!isMoved) moved.asReversed().forEach { (source, target) -> target.moveWithoutReplacing(source) }
        moved.forEach { (source, target) ->
            ownWrites.record(source)
            ownWrites.record(target)
        }
        deleteFolder(if (isMoved) path else newPath)
        refresh()
        return isMoved
    }

    /**
     * Puts the file called [name] in [folder] (empty for none), keeping what its entry holds. The new entry is written
     * before the old ones go, so a write that fails ([java.io.IOException], thrown on) changes nothing.
     */
    @EditorMutation
    public fun move(name: String, folder: String) {
        val files = filesOf(name)
        val text = files.firstOrNull()?.readText() ?: DEFAULT_TEXT
        val target = entryOf(name, folder)
        val isKept = folder.isNotEmpty() || text != DEFAULT_TEXT
        if (isKept) {
            target.parentFile.mkdirs()
            target.writeTextAtomically(text)
            ownWrites.record(target)
        }
        files.filter { !isKept || it.absoluteFile != target.absoluteFile }.forEach { file ->
            file.delete()
            ownWrites.record(file)
        }
        refresh()
    }

    /**
     * Renames the entries of the file called [name], if it has any, to [newName]'s in the same folders, replacing an entry
     * left there for a file of that name that no longer exists.
     */
    @EditorMutation
    public fun rename(name: String, newName: String) {
        filesOf(name).forEach { file ->
            val target = File(file.parentFile, "$newName.$EXTENSION")
            target.delete()
            if (file.moveWithoutReplacing(target)) {
                ownWrites.record(file)
                ownWrites.record(target)
            }
        }
        refresh()
    }

    /**
     * The entries of the file called [name], as of the last [refresh] (which every change here makes): one, or none for a
     * file in no folder, or several when it was put in more than one.
     */
    public fun filesOf(name: String): List<File> = filesByName[name].orEmpty()

    /**
     * Where the entry of the file called [name] goes in [folder].
     */
    protected fun entryOf(name: String, folder: String): File = File(File(directory, folder), "$name.$EXTENSION")

    public companion object {

        /**
         * What an entry holds when Kubriko Creator keeps nothing about its file besides its folder.
         */
        public const val DEFAULT_TEXT: String = "{}\n"
        private const val EXTENSION = "json"

        /**
         * The entries in [directoryPath] of the project at [rootDirectory], by each file's name, in order of their paths.
         * Blocks.
         */
        public fun filesByNameIn(rootDirectory: File, directoryPath: String): Map<String, List<File>> = File(rootDirectory, directoryPath)
            .walkTopDown()
            .filter { it.isFile && it.extension == EXTENSION }
            .sortedBy { it.invariantSeparatorsPath }
            .groupBy { it.nameWithoutExtension }
    }
}

/**
 * The virtual folders of a kind of file ([VirtualFolderStore]).
 *
 * @property folders Every folder, as paths (`enemies/flying`), empty ones too.
 * @property folderOf The folder of each file in one, by the file's name.
 */
public interface VirtualFolders {
    public val folders: Set<String>
    public val folderOf: Map<String, String>
}
