/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.screenplay

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.moveWithoutReplacing
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import com.pandulapeter.kubrikoCreator.project.OwnWrites
import com.pandulapeter.kubrikoCreator.project.ProjectTree
import com.pandulapeter.kubrikoCreator.project.withLineBreaksOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.IOException

/**
 * The screenplay's pages: plain Markdown files in `screenplay/` at the project's root ([PageNaming]), shared with the user,
 * who may write them anywhere. Folders are real folders. The names are known from the last [refresh] and Kubriko Creator's
 * own writes, so validating a command reads nothing; every other call blocks, so it runs off the main thread, and acts on
 * the folder as it is, since the user changes it too.
 */
public class ScreenplayStore(
    public val rootDirectory: File,
    private val ownWrites: OwnWrites = OwnWrites(),
    private val movedFiles: MovedFiles = MovedFiles(),
) {
    private val folder get() = File(rootDirectory, PageNaming.FOLDER)

    @Volatile
    private var knownNames: List<String>? = null

    @Volatile
    private var knownFolders: Set<String>? = null
    private val refreshLock = Any()
    private val _current = MutableStateFlow(emptyList<String>())
    private val _currentFolders = MutableStateFlow(emptySet<String>())
    private val _revision = MutableStateFlow(0)

    init {
        // Pages are written only through this store, which follows them itself; the other files among them (images) and
        // the settings are written by whichever command needs to, and heard of here.
        val settings = File(rootDirectory, ScreenplaySettings.FILE).absoluteFile
        ownWrites.addListener { file ->
            val isAmongPages = file.startsWith(folder.absoluteFile) && file.extension != PageNaming.EXTENSION
            if (isAmongPages || file == settings) _revision.update { it + 1 }
        }
    }

    /**
     * The page names as last known, for what shows them.
     */
    public val current: StateFlow<List<String>> = _current.asStateFlow()

    /**
     * The folders as last known, for what shows them.
     */
    public val currentFolders: StateFlow<Set<String>> = _currentFolders.asStateFlow()

    /**
     * Raised by every write and [refresh], so that what shows a page reads it again.
     */
    public val revision: StateFlow<Int> = _revision.asStateFlow()

    /**
     * Every page's name as last known ([refresh]), the first page first, then by name; none before the first refresh,
     * since this is what validates on the main thread.
     */
    public fun names(): List<String> = knownNames.orEmpty()

    /**
     * Reads the page names from the folder again.
     */
    public fun refresh(): Unit = synchronized(refreshLock) {
        // One at a time, so that a scan the watcher started before a command's write can't publish after the write's.
        val names = scanNames()
        val folders = scanFolders()
        knownNames = names
        knownFolders = folders
        _current.value = names
        _currentFolders.value = folders
        _revision.update { it + 1 }
    }

    /**
     * Every page's name as the folder has it now, the first page first, then by name.
     */
    public fun scanNames(): List<String> = folder.takeIf { it.isDirectory }?.let(ProjectTree::walk)
        ?.filter { it.isFile && it.extension == PageNaming.EXTENSION }
        ?.map { nameOf(it) }
        ?.sortedWith(compareBy<String> { it != PageNaming.FIRST_PAGE }.thenBy { it.lowercase() }.thenBy { it })
        ?.toList()
        .orEmpty()

    /**
     * Every folder of the screenplay as last known ([refresh]), as paths (`levels/forest`), empty ones too.
     */
    public fun folders(): Set<String> = knownFolders.orEmpty()

    /**
     * Every folder of the screenplay as the folder has it now.
     */
    public fun scanFolders(): Set<String> = folder.takeIf { it.isDirectory }?.let(ProjectTree::walk)
        ?.filter { it.isDirectory && it != folder }
        ?.mapTo(sortedSetOf()) { it.relativeTo(folder).invariantSeparatorsPath }
        .orEmpty()

    /**
     * The name of the page [name] names ignoring case, if there is one: what a new page may not be called.
     */
    public fun existingName(name: String): String? = names().firstOrNull { it.equals(name, ignoreCase = true) }

    /**
     * The name of the page [name] names ignoring case, as the folder has it now: what a page is checked against just
     * before it is written, since the user may have made one meanwhile.
     */
    public fun scanExistingName(name: String): String? = scanNames().firstOrNull { it.equals(name, ignoreCase = true) }

    public fun read(name: String): String? = fileOf(name).takeIf { it.isFile }?.readText()

    /**
     * Writes the page [name] with [text], keeping the line breaks the file has, and creating its folders.
     */
    @EditorMutation
    public fun write(name: String, text: String) {
        val file = fileOf(name)
        val previous = file.takeIf { it.isFile }?.readText()
        file.parentFile.mkdirs()
        file.writeTextAtomically(previous?.let(text::withLineBreaksOf) ?: text)
        ownWrites.record(file)
        if (previous == null) refresh() else _revision.update { it + 1 }
    }

    /**
     * Creates the folder at [path] with its parents.
     */
    @EditorMutation
    public fun createFolder(path: String) {
        if (File(folder, path).mkdirs()) refresh()
    }

    /**
     * Deletes the folder at [path], with the folders in it, if no file is in it any more.
     */
    @EditorMutation
    public fun deleteFolder(path: String) {
        val directory = File(folder, path)
        if (directory.isDirectory && ProjectTree.walk(directory).none { it.isFile } && directory.deleteRecursively()) refresh()
    }

    /**
     * Deletes the page [name], leaving its folder, as a folder is deleted only by [deleteFolder].
     */
    @EditorMutation
    public fun delete(name: String) {
        val file = fileOf(name)
        if (!file.delete() && file.exists()) throw IOException("${file.path} couldn't be deleted.")
        ownWrites.record(file)
        refresh()
    }

    /**
     * Moves the page [oldName] to [newName], into another folder if its path says so.
     *
     * @return Whether it was moved: Windows refuses to move a file another program has open.
     */
    @EditorMutation
    public fun move(oldName: String, newName: String): Boolean {
        val source = fileOf(oldName)
        val target = fileOf(newName)
        if (!source.moveWithoutReplacing(target)) return false
        ownWrites.record(source)
        ownWrites.record(target)
        movedFiles.record(PageNaming.pathOf(oldName), PageNaming.pathOf(newName))
        refresh()
        return true
    }

    public fun fileOf(name: String): File = File(folder, "$name.${PageNaming.EXTENSION}")

    private fun nameOf(file: File) = file.relativeTo(folder).invariantSeparatorsPath.removeSuffix(".${PageNaming.EXTENSION}")
}
