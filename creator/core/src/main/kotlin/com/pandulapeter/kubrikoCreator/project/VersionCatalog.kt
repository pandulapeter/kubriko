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
 * Line-based edits of the entries of a Gradle version catalog (`gradle/libs.versions.toml`), which leave every other line
 * as it was.
 */
public object VersionCatalog {
    public const val FILE: String = "gradle/libs.versions.toml"
    public const val VERSIONS: String = "[versions]"
    public const val LIBRARIES: String = "[libraries]"
    public const val PLUGINS: String = "[plugins]"
    private const val BUNDLES = "[bundles]"
    private val entryPattern = Regex("""^\s*([A-Za-z0-9_-]+)\s*=""")
    private const val VERSION_NAME = "kubriko"
    private val versionPattern = Regex("""^\s*$VERSION_NAME\s*=\s*"([^"]*)"\s*$""")
    private val libraryPattern = Regex("""^\s*([A-Za-z0-9_-]+)\s*=\s*\{(.*)\}\s*$""")
    private val groupPattern = Regex("""\bgroup\s*=\s*"([^"]+)"""")
    private val namePattern = Regex("""\bname\s*=\s*"([^"]+)"""")
    private val modulePattern = Regex("""\bmodule\s*=\s*"([^":]+):([^"]+)"""")

    /**
     * The catalog's `kubriko` version, or null if it has none.
     */
    internal fun kubrikoVersion(text: String): String? {
        val lines = text.lines()
        return section(lines, VERSIONS).firstNotNullOfOrNull { versionPattern.find(lines[it])?.groupValues?.get(1) }
    }

    internal fun withKubrikoVersion(text: String, version: String): String {
        val lines = text.lines().toMutableList()
        section(lines, VERSIONS).firstOrNull { versionPattern.matches(lines[it]) }?.let { index ->
            lines[index] = lines[index].replaceRange(versionPattern.find(lines[index])!!.groups[1]!!.range, version)
        }
        return lines.joinToString("\n")
    }

    /**
     * The alias of each Kubriko library the catalog has an entry for.
     */
    internal fun kubrikoLibraries(text: String): Map<KubrikoArtifact, String> {
        val lines = text.lines()
        return section(lines, LIBRARIES).mapNotNull { index -> library(lines[index]) }.toMap()
    }

    /**
     * [text] with an entry for each of [added] (named `kubriko-<catalogName>`, using the `kubriko` version) and without
     * the entries with the [removed] aliases. New entries go where they keep the entries sorted if they are.
     */
    internal fun withKubrikoLibraries(text: String, added: Collection<KubrikoArtifact>, removed: Collection<String>): String {
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
     * The aliases of the bundles of [text] that hold one of the libraries with [aliases].
     */
    internal fun bundlesHolding(text: String, aliases: Collection<String>): List<String> {
        val lines = text.lines()
        return section(lines, BUNDLES).mapNotNull { index ->
            val (alias, entries) = lines[index].split('=', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
            alias.trim().takeIf { bundle -> bundle.isNotEmpty() && aliases.any { "\"$it\"" in entries } }
        }
    }

    /**
     * The alias Kubriko Creator gives [artifact] in a catalog that has no entry for it yet.
     */
    internal fun aliasOf(artifact: KubrikoArtifact) = "kubriko-${artifact.catalogName}"

    /**
     * How the build file refers to the library with [alias]: `libs.` followed by it, with dashes and underscores as dots.
     */
    internal fun accessorOf(alias: String) = "libs." + alias.replace('-', '.').replace('_', '.')

    /**
     * Whether the [header] (such as [PLUGINS]) of [text] has an entry named [alias].
     */
    public fun hasEntry(text: String, header: String, alias: String): Boolean {
        val lines = text.lines()
        return section(lines, header).any { entryPattern.find(lines[it])?.groupValues?.get(1) == alias }
    }

    /**
     * The value of the entry named [alias] in the [header] of [text], trimmed, or null if it has none.
     */
    public fun valueOf(text: String, header: String, alias: String): String? {
        val lines = text.lines()
        return section(lines, header).firstOrNull { entryPattern.find(lines[it])?.groupValues?.get(1) == alias }
            ?.let { lines[it].substringAfter('=').trim() }
    }

    /**
     * [text] with the entry [alias] = [value] in its [header], which is added at the end if the catalog has none, or [text]
     * itself if it has an entry of that name. The entry goes where it keeps the entries sorted if they are.
     */
    public fun withEntry(text: String, header: String, alias: String, value: String): String {
        if (hasEntry(text, header, alias)) return text
        val lines = text.lines().toMutableList()
        if (lines.none { it.trim() == header }) {
            val end = lines.indexOfLast { it.isNotBlank() } + 1
            lines.subList(end, lines.size).clear()
            lines.addAll(listOf("", header, ""))
        }
        val entries = section(lines, header).filter { entryPattern.containsMatchIn(lines[it]) }
        val aliases = entries.map { entryPattern.find(lines[it])!!.groupValues[1] }
        val position = when {
            entries.isEmpty() -> lines.indexOfFirst { it.trim() == header } + 1
            aliases == aliases.sorted() -> entries.firstOrNull { entryPattern.find(lines[it])!!.groupValues[1] > alias } ?: (entries.last() + 1)
            else -> entries.last() + 1
        }
        lines.add(position, "$alias = $value")
        return lines.joinToString("\n")
    }

    /**
     * [text] without the entry named [alias] in its [header].
     */
    public fun withoutEntry(text: String, header: String, alias: String): String {
        val entries = section(text.lines(), header)
        return text.lines().filterIndexed { index, line -> index !in entries || entryPattern.find(line)?.groupValues?.get(1) != alias }.joinToString("\n")
    }

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
