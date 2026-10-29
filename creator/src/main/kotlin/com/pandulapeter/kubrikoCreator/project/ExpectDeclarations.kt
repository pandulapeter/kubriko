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
            .onEnter { it == rootDirectory || it.isWithinSources() || it.name !in ignoredDirectoryNames }
            .filter { it.isFile && it.extension == "kt" && it.isWithinSources() }
            .toSet()
        declaresExpectBySource.keys.retainAll(sources)
        return sources.any { file ->
            val stamp = FileStamp.of(file)
            declaresExpectBySource[file]?.takeIf { it.first == stamp }?.second
                ?: declaresExpect(file.readText()).also { declaresExpectBySource[file] = stamp to it }
        }
    }

    private fun File.isWithinSources() = SOURCE_DIRECTORY in relativeTo(rootDirectory).invariantSeparatorsPath.split('/').dropLast(1)

    companion object {
        private const val SOURCE_DIRECTORY = "src"
        private val ignoredDirectoryNames = setOf(
            "build", "out", ".gradle", ".git", ".idea", ".fleet", ".vscode", ".kotlin", ".kubriko", ".cxx", "kotlin-js-store", "node_modules", "Pods",
            "DerivedData",
        )
        private val declaration = Regex("""(?m)^[\t ]*(?:@\S+[\t ]+)*(?:[a-z]+[\t ]+)*expect[\t ]+(?:[a-z]+[\t ]+)*(?:fun|class|object|interface|val|var|typealias)\b""")

        fun declaresExpect(text: String) = declaration.containsMatchIn(text)
    }
}
