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

import com.pandulapeter.kubrikoCreator.project.ProjectTree
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
