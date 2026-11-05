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
import com.pandulapeter.kubrikoCreator.project.ResourceScanner
import com.pandulapeter.kubrikoCreator.project.withLineBreaksOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/**
 * The project's Markdown files that aren't pages (`ResourceGroup.DOCUMENTS`), by their paths from the project's root
 * ([DocumentNaming]), which belong to the user. The paths are known from the last resource scan ([refresh]) and Kubriko
 * Creator's own writes, so validating reads nothing; every other call blocks, so it runs off the main thread, and acts on
 * the files as they are, since the user changes them too.
 */
public class DocumentStore(
    private val rootDirectory: File,
    private val ownWrites: OwnWrites = OwnWrites(),
) {
    @Volatile
    private var knownPaths: List<String> = emptyList()
    private val pathsLock = Any()
    private val _revision = MutableStateFlow(0)
    private val _structureRevision = MutableStateFlow(0)

    /**
     * Raised by every write and by [onChangedOutside], so that what shows a document reads it again.
     */
    public val revision: StateFlow<Int> = _revision.asStateFlow()

    /**
     * Raised whenever Kubriko Creator created, deleted, or moved a document, which the watcher never reports, so that the
     * resource tree is scanned again.
     */
    public val structureRevision: StateFlow<Int> = _structureRevision.asStateFlow()

    /**
     * Every document's path as last known, sorted.
     */
    public fun paths(): List<String> = knownPaths

    public fun isKnown(path: String): Boolean = path in knownPaths

    /**
     * The path of the document [path] names ignoring case, if there is one: where a new document may not go, since macOS
     * and Windows tell no difference.
     */
    public fun existingPath(path: String): String? = knownPaths.firstOrNull { it.equals(path, ignoreCase = true) }

    /**
     * Takes the paths a resource scan found.
     */
    public fun refresh(paths: List<String>) {
        synchronized(pathsLock) { knownPaths = paths }
    }

    /**
     * The text of the document at [path], or null if there is no such file or it isn't UTF-8 text, which writing it
     * back would change.
     */
    public fun read(path: String): String? {
        val file = fileOf(path).takeIf { it.isFile } ?: return null
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(file.readBytes()))
                .toString()
        } catch (_: CharacterCodingException) {
            null
        } catch (_: IOException) {
            null
        }
    }

    /**
     * Creates the document at [path] with [text] as it is, and the folders it needs.
     *
     * @return Whether it was created: never over a file that is there, whatever its case.
     */
    @EditorMutation
    public fun create(path: String, text: String): Boolean {
        val file = fileOf(path)
        if (file.exists()) return false
        file.parentFile?.mkdirs()
        file.writeTextAtomically(text)
        ownWrites.record(file)
        updatePaths { it + path }
        return true
    }

    /**
     * Writes the document at [path] with [text], keeping the line breaks the file has.
     */
    @EditorMutation
    public fun write(path: String, text: String) {
        val file = fileOf(path)
        val previous = file.takeIf { it.isFile }?.readText()
        file.writeTextAtomically(previous?.let(text::withLineBreaksOf) ?: text)
        ownWrites.record(file)
        _revision.update { it + 1 }
    }

    /**
     * Deletes the document at [path], leaving its folder.
     */
    @EditorMutation
    public fun delete(path: String) {
        val file = fileOf(path)
        if (!file.delete() && file.exists()) throw IOException("${file.path} couldn't be deleted.")
        ownWrites.record(file)
        updatePaths { it - path }
    }

    /**
     * Moves the file at [from] to [to] (both from the project's root, so into or out of the screenplay too), creating
     * the folders [to] needs, and never over a file that is there, unless it is the same file (only the case of its name
     * changes on macOS or Windows).
     *
     * @return Whether it was moved: Windows refuses to move a file another program has open.
     */
    @EditorMutation
    public fun move(from: String, to: String): Boolean {
        val source = fileOf(from)
        val target = fileOf(to)
        if (!source.moveWithoutReplacing(target)) return false
        ownWrites.record(source)
        ownWrites.record(target)
        updatePaths { paths -> (paths - from).let { if (ResourceScanner.isDocument(to)) it + to else it } }
        return true
    }

    /**
     * Follows a document changed outside Kubriko Creator.
     */
    public fun onChangedOutside() {
        _revision.update { it + 1 }
    }

    public fun fileOf(path: String): File = File(rootDirectory, path)

    private fun updatePaths(transform: (List<String>) -> List<String>) {
        synchronized(pathsLock) { knownPaths = transform(knownPaths).distinct().sorted() }
        _revision.update { it + 1 }
        _structureRevision.update { it + 1 }
    }
}
