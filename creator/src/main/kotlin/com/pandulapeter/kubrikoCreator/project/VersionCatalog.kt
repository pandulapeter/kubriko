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

/**
 * Line-based edits of the Kubriko entries of a Gradle version catalog (`gradle/libs.versions.toml`), which leave every
 * other line as it was.
 */
internal object VersionCatalog {
    private const val VERSIONS = "[versions]"
    private const val LIBRARIES = "[libraries]"
    private const val VERSION_NAME = "kubriko"
    private val versionPattern = Regex("""^\s*$VERSION_NAME\s*=\s*"([^"]*)"\s*$""")
    private val libraryPattern = Regex("""^\s*([A-Za-z0-9_-]+)\s*=\s*\{(.*)\}\s*$""")
    private val groupPattern = Regex("""\bgroup\s*=\s*"([^"]+)"""")
    private val namePattern = Regex("""\bname\s*=\s*"([^"]+)"""")
    private val modulePattern = Regex("""\bmodule\s*=\s*"([^":]+):([^"]+)"""")

    /**
     * The catalog's `kubriko` version, or null if it has none.
     */
    fun kubrikoVersion(text: String): String? {
        val lines = text.lines()
        return section(lines, VERSIONS).firstNotNullOfOrNull { versionPattern.find(lines[it])?.groupValues?.get(1) }
    }

    fun withKubrikoVersion(text: String, version: String): String {
        val lines = text.lines().toMutableList()
        section(lines, VERSIONS).firstOrNull { versionPattern.matches(lines[it]) }?.let { index ->
            lines[index] = lines[index].replaceRange(versionPattern.find(lines[index])!!.groups[1]!!.range, version)
        }
        return lines.joinToString("\n")
    }

    /**
     * The alias of each Kubriko library the catalog has an entry for.
     */
    fun kubrikoLibraries(text: String): Map<KubrikoArtifact, String> {
        val lines = text.lines()
        return section(lines, LIBRARIES).mapNotNull { index -> library(lines[index]) }.toMap()
    }

    /**
     * [text] with an entry for each of [added] (named `kubriko-<catalogName>`, using the `kubriko` version) and without
     * the entries with the [removed] aliases. New entries go where they keep the entries sorted if they are.
     */
    fun withKubrikoLibraries(text: String, added: Collection<KubrikoArtifact>, removed: Collection<String>): String {
        val libraries = section(text.lines(), LIBRARIES)
        val lines = text.lines().filterIndexed { index, line ->
            index !in libraries || libraryPattern.find(line)?.groupValues?.get(1) !in removed
        }.toMutableList()
        if (lines.none { it.trim() == LIBRARIES }) lines.addAll(listOf("", LIBRARIES))
        added.forEach { artifact ->
            val entries = section(lines, LIBRARIES).filter { libraryPattern.matches(lines[it]) }
            val alias = aliasOf(artifact)
            val line = "$alias = { group = \"${KubrikoArtifact.GROUP}\", name = \"${artifact.artifactId}\", version.ref = \"$VERSION_NAME\" }"
            val aliases = entries.map { libraryPattern.find(lines[it])!!.groupValues[1] }
            val position = when {
                entries.isEmpty() -> lines.indexOfFirst { it.trim() == LIBRARIES } + 1
                aliases == aliases.sorted() -> entries.firstOrNull { libraryPattern.find(lines[it])!!.groupValues[1] > alias } ?: (entries.last() + 1)
                else -> entries.last() + 1
            }
            lines.add(position, line)
        }
        return lines.joinToString("\n")
    }

    /**
     * The alias Kubriko Creator gives [artifact] in a catalog that has no entry for it yet.
     */
    fun aliasOf(artifact: KubrikoArtifact) = "kubriko-${artifact.catalogName}"

    /**
     * How the build file refers to the library with [alias]: `libs.` followed by it, with dashes and underscores as dots.
     */
    fun accessorOf(alias: String) = "libs." + alias.replace('-', '.').replace('_', '.')

    private fun library(line: String): Pair<KubrikoArtifact, String>? {
        val match = libraryPattern.find(line) ?: return null
        val body = match.groupValues[2]
        val module = modulePattern.find(body)
        val group = module?.groupValues?.get(1) ?: groupPattern.find(body)?.groupValues?.get(1)
        val name = module?.groupValues?.get(2) ?: namePattern.find(body)?.groupValues?.get(1)
        if (group != KubrikoArtifact.GROUP || name == null) return null
        return KubrikoArtifact.ofArtifactId(name)?.let { it to match.groupValues[1] }
    }

    /**
     * The indices of the lines in the section with [header], without the header itself.
     */
    private fun section(lines: List<String>, header: String): IntRange {
        val start = lines.indexOfFirst { it.trim() == header }
        if (start < 0) return IntRange.EMPTY
        val end = (start + 1 until lines.size).firstOrNull { lines[it].trimStart().startsWith("[") } ?: lines.size
        return start + 1 until end
    }
}
