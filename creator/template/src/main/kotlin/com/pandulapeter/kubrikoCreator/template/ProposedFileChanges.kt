/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import com.pandulapeter.kubrikoCreator.project.withLineBreaksOf
import com.pandulapeter.kubrikoCreator.project.withUnixLineBreaks
import java.io.File
import java.io.IOException

/**
 * Makes this change to [file], whose content is [raw] (empty if it doesn't exist), keeping its line breaks.
 */
@EditorMutation
internal fun ProposedFileChange.writeTo(file: File, raw: String) {
    when (after) {
        null -> file.delete()
        else -> {
            file.parentFile?.mkdirs()
            file.writeTextAtomically(after.withLineBreaksOf(raw))
        }
    }
}

/**
 * Makes every one of [changes] in [rootDirectory] if each file is still as the change found it, and none of them otherwise;
 * a write that fails puts back the files written before it, and throws.
 *
 * @return Whether they were made.
 */
@EditorMutation
internal fun applyAll(rootDirectory: File, changes: List<ProposedFileChange>): Boolean {
    val raws = changes.map { change ->
        val file = File(rootDirectory, change.path)
        when {
            change.isNew -> if (file.exists()) return false else ""
            !file.isFile -> return false
            else -> file.readText().takeIf { it.withUnixLineBreaks() == change.before } ?: return false
        }
    }
    val written = ArrayList<Int>()
    try {
        changes.forEachIndexed { index, change ->
            change.writeTo(File(rootDirectory, change.path), raws[index])
            written += index
        }
    } catch (exception: IOException) {
        written.forEach { index -> runCatching { restore(File(rootDirectory, changes[index].path), changes[index], raws[index]) } }
        throw exception
    }
    return true
}

/**
 * Puts back what [change] found in [file] ([raw]), deleting a file it created.
 */
@EditorMutation
private fun restore(file: File, change: ProposedFileChange, raw: String) {
    if (change.isNew) file.delete() else file.writeTextAtomically(raw)
}
