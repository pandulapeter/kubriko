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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Notices files changing in a project, so that edits made in IntelliJ, or by git, show up in Kubriko Creator. It
 * compares a fingerprint of every file's path, size, and modification time once per [intervalInMilliseconds], which
 * is cheap for a game's few hundred files and works the same on every platform (unlike the JDK's file watching,
 * which polls slowly on macOS anyway).
 *
 * @param hiddenDirectory Creator's own generated code, relative to [rootDirectory], whose changes are left out.
 */
internal class ProjectWatcher(
    private val rootDirectory: File,
    private val hiddenDirectory: String?,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val intervalInMilliseconds: Long = 1_000,
) {

    fun start(onChange: (Set<ProjectChange>) -> Unit): AutoCloseable {
        val job = scope.launch {
            var previous = withContext(ioDispatcher) { fingerprint(rootDirectory, hiddenDirectory) }
            while (isActive) {
                delay(intervalInMilliseconds)
                val current = withContext(ioDispatcher) { fingerprint(rootDirectory, hiddenDirectory) }
                val changes = ProjectChange.entries.filter { current[it] != previous[it] }.toSet()
                previous = current
                if (changes.isNotEmpty()) onChange(changes)
            }
        }
        return AutoCloseable { job.cancel() }
    }

    companion object {
        private val ignoredDirectoryNames = setOf("build", ".gradle", ".git", ".idea", ".kotlin", "kotlin-js-store", "node_modules")

        /**
         * A value per area that changes whenever a file in it is added, removed, resized, or modified.
         */
        internal fun fingerprint(rootDirectory: File, hiddenDirectory: String?): Map<ProjectChange, Int> {
            val hashes = ProjectChange.entries.associateWith { 1 }.toMutableMap()
            rootDirectory.walkTopDown()
                .onEnter { it == rootDirectory || (it.name !in ignoredDirectoryNames && it.relativeTo(rootDirectory).invariantSeparatorsPath != hiddenDirectory) }
                .filter { it.isFile }
                .forEach { file ->
                    val path = file.relativeTo(rootDirectory).invariantSeparatorsPath
                    val area = areaOf(path) ?: return@forEach
                    hashes[area] = 31 * hashes.getValue(area) + (path.hashCode() xor file.lastModified().hashCode() xor file.length().hashCode())
                }
            return hashes
        }

        internal fun areaOf(path: String) = when {
            path.startsWith(".kubriko/actors/") -> ProjectChange.ACTOR_TYPE_DEFINITIONS
            ResourceScanner.classify(path) == ResourceGroup.SCENES -> ProjectChange.SCENES
            path.startsWith("src/") || "/src/" in path -> ProjectChange.SOURCES
            else -> null
        }
    }
}
