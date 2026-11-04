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
 * The project's Markdown files that aren't pages (`ResourceGroup.DOCUMENTS`), by their paths from the project's root,
 * which belong to the user: Kubriko Creator only edits the ones it knows ([paths], from the last resource scan, so
 * validating reads nothing), and moves one into the screenplay when asked. Every other call blocks, so it runs off the
 * main thread.
 */
public class DocumentStore(
    private val rootDirectory: File,
    private val ownWrites: OwnWrites,
    private val paths: () -> List<String>,
) {
    private val _revision = MutableStateFlow(0)

    /**
     * Raised by every write and by [onChangedOutside], so that what shows a document reads it again.
     */
    public val revision: StateFlow<Int> = _revision.asStateFlow()

    /**
     * Every document's path as last scanned.
     */
    public fun paths(): List<String> = paths.invoke()

    public fun isKnown(path: String): Boolean = path in paths()

    public fun read(path: String): String? = fileOf(path).takeIf { it.isFile }?.readText()

    /**
     * Writes the document at [path] with [text], keeping the line breaks the file has.
     */
    @EditorMutation
    public fun write(path: String, text: String) {
        val file = fileOf(path)
        val previous = file.takeIf { it.isFile }?.readText()
        file.parentFile?.mkdirs()
        file.writeTextAtomically(previous?.let(text::withLineBreaksOf) ?: text)
        ownWrites.record(file)
        _revision.update { it + 1 }
    }

    /**
     * Moves the file at [from] to [to] (both from the project's root), creating the folders [to] needs, and never over a
     * file that is there.
     *
     * @return Whether it was moved: Windows refuses to move a file another program has open.
     */
    @EditorMutation
    public fun move(from: String, to: String): Boolean {
        val source = fileOf(from)
        val target = fileOf(to)
        val isMoved = try {
            target.parentFile?.mkdirs()
            Files.move(source.toPath(), target.toPath())
            true
        } catch (_: IOException) {
            false
        }
        if (!isMoved) return false
        ownWrites.record(source)
        ownWrites.record(target)
        _revision.update { it + 1 }
        return true
    }

    /**
     * Follows a document changed outside Kubriko Creator.
     */
    public fun onChangedOutside() {
        _revision.update { it + 1 }
    }

    public fun fileOf(path: String): File = File(rootDirectory, path)
}
