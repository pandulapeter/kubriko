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
 * How every walk through a project's files goes, and what it leaves out.
 */
public object ProjectTree {

    /**
     * The folders build tools and version control keep their own files in.
     */
    internal val ignoredDirectoryNames = setOf("build", ".gradle", ".git", ".idea", ".kotlin", "kotlin-js-store", "node_modules")

    /**
     * The folders IDEs and native builds keep their own files in, which a package or a resource folder may be called too.
     */
    private val toolDirectoryNames = setOf("out", ".fleet", ".vscode", ".cxx", "Pods", "DerivedData")

    /**
     * Every file and folder under [root], top down. Build, tool, and version control folders ([ignoredDirectoryNames] and
     * the IDEs' and native builds' own) are skipped outside source folders only, so a source package or a resource
     * folder may have any name.
     *
     * @param skipped The names of more folders to skip, inside source folders too.
     * @param hiddenDirectory A folder, relative to [root], to skip.
     */
    public fun walk(root: File, skipped: Set<String> = emptySet(), hiddenDirectory: String? = null): Sequence<File> = root.walkTopDown().onEnter { directory ->
        directory == root || (
            directory.name !in skipped &&
                (hiddenDirectory == null || directory.relativeTo(root).invariantSeparatorsPath != hiddenDirectory) &&
                (isWithinSources(root, directory) || !isIgnored(directory))
            )
    }

    /**
     * Whether [file] is inside the `src` folder of one of [root]'s modules, at any depth.
     */
    internal fun isWithinSources(root: File, file: File) = "src" in file.relativeTo(root).invariantSeparatorsPath.split('/').dropLast(1)

    private fun isIgnored(directory: File) = directory.name in ignoredDirectoryNames || directory.name in toolDirectoryNames
}
