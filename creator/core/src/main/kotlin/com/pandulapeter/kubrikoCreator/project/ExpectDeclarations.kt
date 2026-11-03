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
public class ExpectDeclarations(
    private val rootDirectory: File,
) {
    private val declaresExpectBySource = HashMap<File, Pair<FileStamp, Boolean>>()

    @Synchronized
    public fun any(): Boolean {
        val sources = ProjectTree.walk(rootDirectory, skipped = setOf(CREATOR_DIRECTORY))
            .filter { it.isFile && it.extension == "kt" && ProjectTree.isWithinSources(rootDirectory, it) }
            .toSet()
        declaresExpectBySource.keys.retainAll(sources)
        return sources.any { file ->
            val stamp = FileStamp.of(file)
            declaresExpectBySource[file]?.takeIf { it.first == stamp }?.second
                ?: declaresExpect(file.readText()).also { declaresExpectBySource[file] = stamp to it }
        }
    }

    internal companion object {
        private const val CREATOR_DIRECTORY = ".kubriko"
        private val declaration = Regex("""(?m)^[\t ]*(?:@\S+[\t ]+)*(?:[a-z]+[\t ]+)*expect[\t ]+(?:[a-z]+[\t ]+)*(?:fun|class|object|interface|val|var|typealias)\b""")

        internal fun declaresExpect(text: String) = declaration.containsMatchIn(text)
    }
}
