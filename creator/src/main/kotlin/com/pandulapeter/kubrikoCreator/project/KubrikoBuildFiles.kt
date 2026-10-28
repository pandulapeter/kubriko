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
 * Keeps the Kubriko side of an opted-in project's build in step with its code and with Kubriko Creator: the project
 * depends on exactly the Kubriko libraries its sources use (generated code included, so a game without keys has no
 * keyboard plugin), at the version Kubriko Creator itself was built with, which embedded Scene editing needs.
 *
 * Only the Kubriko entries of `gradle/libs.versions.toml`, the lines of `build.gradle.kts` that add them, and Kubriko
 * Creator's own repository blocks in `settings.gradle.kts` (Maven Local, and JitPack for the audio plugin) change; a project whose files don't have that shape is left
 * alone. A project on a newer Kubriko version than Kubriko Creator's keeps it.
 *
 * @param useMavenLocal Whether [kubrikoVersion] is a development build in Maven Local, which the project's settings
 *   then have to look in.
 */
internal class KubrikoBuildFiles(
    private val rootDirectory: File,
    private val kubrikoVersion: String,
    private val useMavenLocal: Boolean,
) {
    /**
     * Brings the build files up to date with the sources.
     *
     * @return What changed, or null if the project's build files don't have the template's shape.
     */
    fun sync(): KubrikoBuildFileChanges? {
        val catalogText = read(CATALOG_FILE) ?: return null
        val buildText = read(BUILD_FILE) ?: return null
        val projectVersion = VersionCatalog.kubrikoVersion(catalogText) ?: return null
        val version = if (compareVersions(projectVersion, kubrikoVersion) > 0) projectVersion else kubrikoVersion
        val used = KubrikoArtifact.usedBy(sources())
        val aliases = VersionCatalog.kubrikoLibraries(catalogText)
        val accessors = KubrikoArtifact.entries.associateWith { VersionCatalog.accessorOf(aliases[it] ?: VersionCatalog.aliasOf(it)) }
        val added = used.filter { !BuildScript.refersTo(buildText, accessors.getValue(it)) }
        val removed = aliases.keys.filter { it !in used && BuildScript.refersTo(buildText, accessors.getValue(it)) }
        val changedBuild = BuildScript.withDependencies(buildText, added.map(accessors::getValue), removed.map(accessors::getValue)) ?: return null
        val unusedAliases = aliases.filter { (artifact, _) -> artifact !in used && !BuildScript.refersTo(changedBuild, accessors.getValue(artifact)) }
        val changedCatalog = VersionCatalog.withKubrikoVersion(
            VersionCatalog.withKubrikoLibraries(catalogText, added.filter { it !in aliases }, unusedAliases.values),
            version,
        )
        write(BUILD_FILE, buildText, changedBuild)
        write(CATALOG_FILE, catalogText, changedCatalog)
        read(SETTINGS_FILE)?.let { settings ->
            val withMavenLocal = if (version == kubrikoVersion) withRepository(settings, MAVEN_LOCAL_REPOSITORY, useMavenLocal, isFirst = true) else settings
            write(SETTINGS_FILE, settings, withRepository(withMavenLocal, JITPACK_REPOSITORY, KubrikoArtifact.AUDIO_PLAYBACK in used, isFirst = false))
        }
        return KubrikoBuildFileChanges(
            addedArtifacts = added.map { it.artifactId }.sorted(),
            removedArtifacts = removed.filter { !BuildScript.refersTo(changedBuild, accessors.getValue(it)) }.map { it.artifactId }.sorted(),
            previousVersion = projectVersion.takeIf { it != version },
            version = version,
            isNewerThanCreator = version != kubrikoVersion,
        )
    }

    private fun sources() = File(rootDirectory, SOURCE_DIRECTORY).walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .map { it.readText() }

    private fun read(path: String) = File(rootDirectory, path).takeIf { it.isFile }?.readText()

    private fun write(path: String, before: String, after: String) {
        if (after != before) File(rootDirectory, path).writeText(after)
    }

    companion object {
        const val CATALOG_FILE = "gradle/libs.versions.toml"
        const val BUILD_FILE = "build.gradle.kts"
        const val SETTINGS_FILE = "settings.gradle.kts"
        private const val SOURCE_DIRECTORY = "src"

        /**
         * The repository block that lets a project use the development builds Kubriko Creator publishes to Maven Local.
         */
        const val MAVEN_LOCAL_REPOSITORY = "        mavenLocal {\n            content { includeGroup(\"io.github.pandulapeter.kubriko\") }\n        }\n"

        /**
         * The repository block that the audio plugin's desktop library needs, for the MP3 decoder it depends on.
         */
        const val JITPACK_REPOSITORY = "        maven(\"https://jitpack.io\") {\n            content { includeGroup(\"com.github.umjammer\") }\n        }\n"

        /**
         * Compares the numbers of two versions, ignoring what follows a dash (`0.8.1-creator` equals `0.8.1`).
         */
        internal fun compareVersions(first: String, second: String): Int {
            fun numbers(version: String) = version.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
            val firstNumbers = numbers(first)
            val secondNumbers = numbers(second)
            for (index in 0 until maxOf(firstNumbers.size, secondNumbers.size)) {
                val difference = firstNumbers.getOrElse(index) { 0 } - secondNumbers.getOrElse(index) { 0 }
                if (difference != 0) return difference
            }
            return 0
        }

        /**
         * [settings] (a `settings.gradle.kts`) with the repository [block] as the first (or last) repository of its
         * dependency resolution, or without it.
         */
        internal fun withRepository(settings: String, block: String, isIncluded: Boolean, isFirst: Boolean): String {
            val hasIt = block in settings
            return when {
                isIncluded && !hasIt -> {
                    val lines = settings.lines()
                    val management = lines.indexOfFirst { it.trimStart().startsWith("dependencyResolutionManagement") }
                    val repositories = (management + 1 until lines.size).firstOrNull { lines[it].trim() == "repositories {" }
                    val indent = repositories?.let { lines[it].takeWhile(Char::isWhitespace) }
                    val end = repositories?.let { (it + 1 until lines.size).firstOrNull { index -> lines[index] == "$indent}" } }
                    val insertion = if (isFirst) repositories?.plus(1) else end
                    if (management < 0 || insertion == null) {
                        settings
                    } else {
                        (lines.take(insertion) + block.trimEnd('\n') + lines.drop(insertion)).joinToString("\n")
                    }
                }
                !isIncluded && hasIt -> settings.replace(block, "")
                else -> settings
            }
        }
    }
}

/**
 * What [KubrikoBuildFiles.sync] changed.
 *
 * @param addedArtifacts The Kubriko libraries the project started depending on.
 * @param removedArtifacts The Kubriko libraries the project no longer depends on.
 * @param previousVersion The Kubriko version the project used before, or null if it didn't change.
 * @param version The Kubriko version the project uses.
 * @param isNewerThanCreator Whether [version] is newer than Kubriko Creator's own, which the project keeps.
 */
internal data class KubrikoBuildFileChanges(
    val addedArtifacts: List<String> = emptyList(),
    val removedArtifacts: List<String> = emptyList(),
    val previousVersion: String? = null,
    val version: String,
    val isNewerThanCreator: Boolean = false,
)
