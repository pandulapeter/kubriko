/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import java.io.File

/**
 * Whether a project's sources (in the `src` folder of any of its modules) declare an `expect` anywhere, reading again only the files that changed since the last
 * check. Text that only looks like a declaration (in a raw string, say) counts too, which errs on the safe side.
 */
internal class ExpectDeclarations(
    private val rootDirectory: File,
) {
    private val declaresExpectBySource = HashMap<File, Pair<FileStamp, Boolean>>()

    @Synchronized
    fun any(): Boolean {
        val sources = rootDirectory.walkTopDown()
            .onEnter { it == rootDirectory || it.name !in ignoredDirectoryNames }
            .filter { it.isFile && it.extension == "kt" && SOURCE_DIRECTORY in it.relativeTo(rootDirectory).invariantSeparatorsPath.split('/') }
            .toSet()
        declaresExpectBySource.keys.retainAll(sources)
        return sources.any { file ->
            val stamp = FileStamp.of(file)
            declaresExpectBySource[file]?.takeIf { it.first == stamp }?.second
                ?: declaresExpect(file.readText()).also { declaresExpectBySource[file] = stamp to it }
        }
    }

    companion object {
        private const val SOURCE_DIRECTORY = "src"
        private val ignoredDirectoryNames = setOf("build", ".gradle", ".git", ".idea", ".kotlin", ".kubriko", "kotlin-js-store", "node_modules")
        private val declaration = Regex("""(?m)^[\t ]*(?:@\S+[\t ]+)*(?:[a-z]+[\t ]+)*expect[\t ]+(?:[a-z]+[\t ]+)*(?:fun|class|object|interface|val|var|typealias)\b""")

        fun declaresExpect(text: String) = declaration.containsMatchIn(text)
    }
}
