/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.foundation.JsonFileStore
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import kotlinx.serialization.KSerializer
import java.io.File
import java.io.IOException

/**
 * One value Kubriko Creator keeps of a project for this computer alone, in a file of `.kubriko/local/` ([FOLDER]). The folder
 * ignores itself in git, since a project Creator didn't make doesn't ignore it. Every call blocks.
 */
internal class LocalProjectStore<T>(
    rootDirectory: File,
    fileName: String,
    serializer: KSerializer<T>,
) {
    private val folder = File(rootDirectory, FOLDER)
    private val store = JsonFileStore(File(folder, fileName), serializer)

    fun read(): T? = when (val read = store.read()) {
        is JsonFileStore.Read.Found -> read.value
        else -> null
    }

    /**
     * Writes [value], returning false when the file was left as it was.
     */
    fun write(value: T): Boolean {
        keepFolderOutOfGit()
        return store.write(value)
    }

    private fun keepFolderOutOfGit() {
        val gitignore = File(folder, ".gitignore")
        if (gitignore.exists()) return
        try {
            folder.mkdirs()
            gitignore.writeTextAtomically("*\n")
        } catch (_: IOException) {
            // The value is still worth keeping; git then only lists the folder as new.
        }
    }

    companion object {
        const val FOLDER = ".kubriko/local"
    }
}
