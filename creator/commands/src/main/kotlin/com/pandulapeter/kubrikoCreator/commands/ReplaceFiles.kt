/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.writeBytesAtomically
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import java.io.File
import java.io.IOException

/**
 * Writes files of the project byte for byte, or deletes them, by their paths from the project's root: how a change that
 * made files Kubriko Creator owns, such as a page's images, is undone exactly. Files written so far are put back when
 * one can't be.
 */
@OptIn(EditorMutation::class)
internal object ReplaceFiles : WriteCommand<ReplaceFiles.Input> {
    override val name = "replace_files"
    override val description = ""
    override val inputSerializer: KSerializer<Input> = NotATool("ReplaceFiles")

    override fun target(context: CommandContext, input: Input) = CommandTarget.Project

    override fun validate(context: CommandContext, input: Input): Rejection? = null

    override suspend fun apply(context: CommandContext, input: Input): Applied = withContext(context.ioDispatcher) {
        val previous = LinkedHashMap<String, ByteArray?>()
        try {
            input.contents.forEach { (path, bytes) ->
                val file = File(context.project.rootDirectory, path)
                previous[path] = file.takeIf { it.isFile }?.readBytes()
                context.write(file, bytes)
            }
        } catch (exception: Exception) {
            previous.forEach { (path, bytes) -> runCatching { context.write(File(context.project.rootDirectory, path), bytes) }.exceptionOrNull()?.let(exception::addSuppressed) }
            throw exception
        }
        Applied(inverse = Invocation(ReplaceFiles, Input(previous)))
    }

    /**
     * Writes a new file at [path] (from the project's root) with [bytes], never over one that is there.
     *
     * @return The file written.
     */
    fun writeNew(context: CommandContext, path: String, bytes: ByteArray): File {
        val file = File(context.project.rootDirectory, path)
        if (file.exists()) throw IOException("${file.path} is there already.")
        context.write(file, bytes)
        return file
    }

    /**
     * Deletes [file], one [writeNew] wrote, when what it was written for couldn't be made.
     */
    fun delete(context: CommandContext, file: File): Unit = context.write(file, null)

    private fun CommandContext.write(file: File, bytes: ByteArray?) {
        if (bytes == null) {
            if (!file.delete() && file.exists()) throw IOException("${file.path} couldn't be deleted.")
        } else {
            file.writeBytesAtomically(bytes)
        }
        scenes.ownWrites.record(file)
        onFilesChanged()
    }

    /**
     * @param contents The bytes of each file, or null to delete it, by its path from the project's root.
     */
    class Input(
        val contents: Map<String, ByteArray?>,
    ) {
        init {
            require(contents.isNotEmpty()) { "Replacing files needs at least one." }
        }
    }
}
