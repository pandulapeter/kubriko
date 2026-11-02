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
 * What every walk through a project's files leaves out.
 */
internal object ProjectTree {

    /**
     * The folders build tools and version control keep their own files in, wherever they are.
     */
    val ignoredDirectoryNames = setOf("build", ".gradle", ".git", ".idea", ".kotlin", "kotlin-js-store", "node_modules")

    /**
     * The folders IDEs and native builds keep their own files in, which a package or a resource folder may be called too.
     */
    private val toolDirectoryNames = setOf("out", ".fleet", ".vscode", ".cxx", "Pods", "DerivedData")

    /**
     * Whether a walk through [rootDirectory] leaves [directory] out: one of [ignoredDirectoryNames] anywhere, or a tool's
     * folder outside the `src` folder of a module.
     */
    fun isIgnored(directory: File, rootDirectory: File) = directory.name in ignoredDirectoryNames ||
        (directory.name in toolDirectoryNames && "src" !in directory.relativeTo(rootDirectory).invariantSeparatorsPath.split('/').dropLast(1))
}
