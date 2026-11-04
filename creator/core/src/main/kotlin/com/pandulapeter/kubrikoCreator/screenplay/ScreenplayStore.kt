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
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import com.pandulapeter.kubrikoCreator.project.OwnWrites
import com.pandulapeter.kubrikoCreator.project.withLineBreaksOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.IOException
import java.nio.file.Files

/**
 * The screenplay's pages: plain Markdown files in `screenplay/` at the project's root ([PageNaming]), shared with the user,
 * who may write them anywhere. Folders are real folders. The names are known from the last [refresh] and Kubriko Creator's
 * own writes, so validating a command reads nothing; every other call blocks, so it runs off the main thread, and acts on
 * the folder as it is, since the user changes it too.
 */
public class ScreenplayStore(
    private val rootDirectory: File,
    private val ownWrites: OwnWrites = OwnWrites(),
) {
    private val folder get() = File(rootDirectory, PageNaming.FOLDER)

    @Volatile
    private var knownNames: List<String>? = null
    private val _current = MutableStateFlow(emptyList<String>())
    private val _revision = MutableStateFlow(0)

    /**
     * The page names as last known, for what shows them.
     */
    public val current: StateFlow<List<String>> = _current.asStateFlow()

    /**
     * Raised by every write and [refresh], so that what shows a page reads it again.
     */
    public val revision: StateFlow<Int> = _revision.asStateFlow()

    /**
     * Every page's name as last known ([refresh]), the first page first, then by name; read from the folder if it isn't
     * known yet.
     */
    public fun names(): List<String> = knownNames ?: scanNames()

    /**
     * Reads the page names from the folder again.
     */
    public fun refresh() {
        val names = scanNames()
        knownNames = names
        _current.value = names
        _revision.update { it + 1 }
    }

    /**
     * Every page's name as the folder has it now, the first page first, then by name.
     */
    public fun scanNames(): List<String> = folder.takeIf { it.isDirectory }?.walkTopDown()
        ?.filter { it.isFile && it.extension == PageNaming.EXTENSION }
        ?.map { nameOf(it) }
        ?.sortedWith(compareBy<String> { it != PageNaming.FIRST_PAGE }.thenBy { it.lowercase() }.thenBy { it })
        ?.toList()
        .orEmpty()

    /**
     * Every folder of the screenplay, as paths (`levels/forest`), empty ones too.
     */
    public fun folders(): Set<String> = folder.takeIf { it.isDirectory }?.walkTopDown()
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
     * Deletes the page [name], and the folders it leaves empty.
     */
    @EditorMutation
    public fun delete(name: String) {
        val file = fileOf(name)
        if (!file.delete() && file.exists()) throw IOException("${file.path} couldn't be deleted.")
        ownWrites.record(file)
        deleteEmptyFoldersAbove(file)
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
        val isMoved = try {
            target.parentFile.mkdirs()
            if (source.canonicalPath.equals(target.canonicalPath, ignoreCase = true)) {
                source.renameTo(target)
            } else {
                Files.move(source.toPath(), target.toPath())
                true
            }
        } catch (_: IOException) {
            false
        }
        if (!isMoved) return false
        ownWrites.record(source)
        ownWrites.record(target)
        deleteEmptyFoldersAbove(source)
        refresh()
        return true
    }

    public fun fileOf(name: String): File = File(folder, "$name.${PageNaming.EXTENSION}")

    private fun nameOf(file: File) = file.relativeTo(folder).invariantSeparatorsPath.removeSuffix(".${PageNaming.EXTENSION}")

    private fun deleteEmptyFoldersAbove(file: File) {
        generateSequence(file.parentFile) { it.parentFile }
            .takeWhile { it != folder && it.startsWith(folder) }
            .forEach { directory -> if (directory.list()?.isEmpty() != true || !directory.delete()) return }
    }
}
