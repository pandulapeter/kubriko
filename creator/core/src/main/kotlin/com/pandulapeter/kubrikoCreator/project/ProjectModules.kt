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
import java.io.IOException

/**
 * Finds a project's Gradle modules the way [ResourceScanner.moduleOf] tells them: every folder with a build file.
 */
public object ProjectModules {

    private const val KUBRIKO_GROUP = "io.github.pandulapeter.kubriko:"
    private val settingsFileNames = setOf("settings.gradle.kts", "settings.gradle")
    private val comments = Regex("""//[^\n]*|/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)

    /**
     * Every module of the project in [rootDirectory], by its folder (empty for the root project).
     *
     * @param snapshot The files as [ProjectWatcher] just found them, whose build files are read instead of walking the
     *   project again; null to walk it.
     */
    public fun find(rootDirectory: File, snapshot: ProjectSnapshot? = null): Map<String, ProjectModule> {
        val catalog = read(File(rootDirectory, VersionCatalog.FILE)).orEmpty()
        val libraries = VersionCatalog.kubrikoLibraries(catalog).values
        val accessors = (libraries + VersionCatalog.bundlesHolding(catalog, libraries).map { "bundles-$it" }).map(VersionCatalog::accessorOf)
        val buildFiles = snapshot?.stamps?.keys?.filter(ResourceScanner::isBuildFile)?.map { File(rootDirectory, it) }
            ?: ProjectTree.walk(rootDirectory, skipped = setOf("src", ".kubriko")).filter { it.isFile && ResourceScanner.isBuildFile(it.name) }.toList()
        return buildFiles.mapNotNull { buildFile ->
            val text = read(buildFile)?.replace(comments, "") ?: return@mapNotNull null
            val folder = buildFile.parentFile
            val path = folder.relativeTo(rootDirectory).invariantSeparatorsPath
            path to ProjectModule(
                usesKubriko = KUBRIKO_GROUP in text || accessors.any { usesAccessor(text, it) },
                isBuildRoot = path.isNotEmpty() && (folder.name == "buildSrc" || settingsFileNames.any { File(folder, it).isFile }),
            )
        }.toMap()
    }

    /**
     * The text of [file], or null when it can't be read, such as one deleted since it was found.
     */
    private fun read(file: File) = try {
        file.takeIf { it.isFile }?.readText()
    } catch (_: IOException) {
        null
    }

    /**
     * Whether [text] uses the catalog accessor [accessor] itself, not one that only starts or ends like it
     * (`libs.kubriko.engine` isn't `libs.kubriko.engineTools`, nor `otherlibs.kubriko.engine`).
     */
    private fun usesAccessor(text: String, accessor: String): Boolean {
        var index = text.indexOf(accessor)
        while (index >= 0) {
            val end = index + accessor.length
            val before = text.getOrNull(index - 1)
            val next = text.getOrNull(end)
            val isWholeBefore = before == null || !(before.isLetterOrDigit() || before == '_' || before == '.')
            val isWholeAfter = next == null || !(next.isLetterOrDigit() || next == '_' || next == '.') || text.startsWith(".get()", end)
            if (isWholeBefore && isWholeAfter) return true
            index = text.indexOf(accessor, index + 1)
        }
        return false
    }
}
