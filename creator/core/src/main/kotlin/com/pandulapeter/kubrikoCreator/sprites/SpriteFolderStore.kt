/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sprites

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.moveWithoutReplacing
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import com.pandulapeter.kubrikoCreator.project.OwnWrites
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * The sidebar's folders of sprites, which are virtual, since Compose resources make accessors only of the images right in
 * `drawable`: each a directory of `.kubriko/sprites`, holding a `<name>.json` file for each sprite in it, named after
 * the image without its extension (sprite names are unique, as their accessors are). A sprite without a file is in no
 * folder. The file holds what Kubriko Creator keeps about the sprite besides its folder ([DEFAULT_TEXT] for nothing).
 * Calls that change it block, so callers run them off the main thread; [state] is read anywhere.
 */
public class SpriteFolderStore(
    private val rootDirectory: File,
    private val ownWrites: OwnWrites,
) {
    /**
     * @param folders Every folder, as paths (`enemies/flying`), empty ones too.
     * @param folderOf The folder of each sprite in one, by the sprite's name.
     */
    public data class State(
        val folders: Set<String> = emptySet(),
        val folderOf: Map<String, String> = emptyMap(),
    )

    private val directory get() = File(rootDirectory, DIRECTORY)
    private val _state = MutableStateFlow(State())
    public val state: StateFlow<State> = _state.asStateFlow()

    @Volatile
    private var filesByName: Map<String, List<File>> = emptyMap()

    /**
     * Reads the folders again. Blocks. A sprite with files in several folders (two branches moving it, merged) is in the
     * first of them by path, and every change to it acts on all of them.
     */
    public fun refresh(): Unit = synchronized(this) {
        val root = directory
        val files = root.walkTopDown().filter { it != root }.sortedBy { it.invariantSeparatorsPath }.toList()
        filesByName = files.filter { it.isFile && it.extension == EXTENSION }.groupBy { it.nameWithoutExtension }
        _state.value = State(
            folders = files.filter { it.isDirectory }.mapTo(sortedSetOf()) { it.relativeTo(root).invariantSeparatorsPath },
            folderOf = filesByName.mapValues { (_, named) -> named.first().parentFile.relativeTo(root).invariantSeparatorsPath },
        )
    }

    @EditorMutation
    public fun createFolder(path: String) {
        File(directory, path).mkdirs()
        refresh()
    }

    /**
     * Deletes the folder at [path] if no sprite is in it any more.
     */
    @EditorMutation
    public fun deleteFolder(path: String) {
        val folder = File(directory, path)
        if (folder.isDirectory && folder.walkTopDown().none { it.isFile } && folder.deleteRecursively()) refresh()
    }

    /**
     * Moves the folder at [path], with every folder and sprite in it, to [newPath].
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
     * Puts the sprite called [name] in [folder] (empty for none), keeping what its file holds. The new file is written
     * before the old ones go, so a write that fails ([java.io.IOException], thrown on) changes nothing.
     */
    @EditorMutation
    public fun move(name: String, folder: String) {
        val files = filesOf(name)
        val text = files.firstOrNull()?.readText() ?: DEFAULT_TEXT
        val target = File(File(directory, folder), "$name.$EXTENSION")
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
     * Renames the files of the sprite called [name], if it has any, to [newName]'s in the same folders, replacing a file
     * left there for an image of that name that no longer exists.
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
     * The files of the sprite called [name], as of the last [refresh] (which every change here makes): one, or none for a
     * sprite in no folder, or several when it was put in more than one.
     */
    public fun filesOf(name: String): List<File> = filesByName[name].orEmpty()

    public companion object {
        public const val DIRECTORY: String = ".kubriko/sprites"
        public const val DEFAULT_TEXT: String = "{}\n"
        private const val EXTENSION = "json"
    }
}
