/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.project.ProjectTree
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * The project's Kotlin files outside what Kubriko Creator generates, which it never rewrites, only names.
 */
internal object HandWrittenKotlin {
    private val creatorDirectories = setOf(".kubriko", "generated")

    /**
     * The text of each hand-written Kotlin file of the project in [rootDirectory], by its path from there, leaving out a
     * file that can't be read.
     */
    fun read(rootDirectory: File): Map<String, String> = ProjectTree.walk(rootDirectory, skipped = creatorDirectories)
        .filter { it.isFile && it.extension == "kt" }
        .mapNotNull { file ->
            try {
                file.relativeTo(rootDirectory).invariantSeparatorsPath to file.readText()
            } catch (_: IOException) {
                null
            }
        }
        .toMap()
}

/**
 * The project's hand-written Kotlin ([HandWrittenKotlin.read]), or what [withKotlinFilesHeld] holds. Reads the disk.
 */
internal fun CommandContext.handWrittenKotlin(): Map<String, String> = heldKotlinFiles ?: HandWrittenKotlin.read(project.rootDirectory)

/**
 * Runs [block] with the hand-written Kotlin read once for every delete it makes, rather than once by each: deletes
 * never rewrite it, so it stays as read until [block] ends.
 */
internal suspend fun <T> CommandContext.withKotlinFilesHeld(block: suspend () -> T): T {
    if (heldKotlinFiles != null) return block()
    heldKotlinFiles = withContext(ioDispatcher) { HandWrittenKotlin.read(project.rootDirectory) }
    try {
        return block()
    } finally {
        heldKotlinFiles = null
    }
}
