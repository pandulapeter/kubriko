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
 * compares every file's length and modification time once per [intervalInMilliseconds], which is cheap for a game's
 * files and works the same on every platform (unlike the JDK's file watching, which polls slowly on macOS anyway).
 * Files that are as Kubriko Creator itself left them ([ownWrites]) don't count as changed.
 *
 * @param hiddenDirectory Creator's own generated code, relative to [rootDirectory], whose changes are left out.
 */
internal class ProjectWatcher(
    private val rootDirectory: File,
    private val hiddenDirectory: String?,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val intervalInMilliseconds: Long = 1_000,
    private val ownWrites: OwnWrites = OwnWrites(),
) {

    fun start(onChange: (Set<ProjectChange>) -> Unit): AutoCloseable {
        val job = scope.launch {
            var previous = withContext(ioDispatcher) { snapshot(rootDirectory, hiddenDirectory) }
            while (isActive) {
                delay(intervalInMilliseconds)
                val current = withContext(ioDispatcher) { snapshot(rootDirectory, hiddenDirectory) }
                val changes = changedAreas(previous, current) { path, stamp -> ownWrites.isOwn(File(rootDirectory, path), stamp) }
                previous = current
                if (changes.isNotEmpty()) onChange(changes)
            }
        }
        return AutoCloseable { job.cancel() }
    }

    companion object {
        private val ignoredDirectoryNames = setOf("build", ".gradle", ".git", ".idea", ".kotlin", "kotlin-js-store", "node_modules")

        /**
         * The stamp of every file in an area, by its path relative to [rootDirectory].
         */
        internal fun snapshot(rootDirectory: File, hiddenDirectory: String?): Map<String, FileStamp> {
            val stamps = HashMap<String, FileStamp>()
            rootDirectory.walkTopDown()
                .onEnter { it == rootDirectory || (it.name !in ignoredDirectoryNames && it.relativeTo(rootDirectory).invariantSeparatorsPath != hiddenDirectory) }
                .filter { it.isFile }
                .forEach { file ->
                    val path = file.relativeTo(rootDirectory).invariantSeparatorsPath
                    if (areaOf(path) != null) stamps[path] = FileStamp(file.length(), file.lastModified())
                }
            return stamps
        }

        /**
         * The areas with a file that was added, removed, resized, or modified between [previous] and [current], other
         * than those [isOwn] says Kubriko Creator changed itself.
         */
        internal fun changedAreas(
            previous: Map<String, FileStamp>,
            current: Map<String, FileStamp>,
            isOwn: (path: String, stamp: FileStamp) -> Boolean = { _, _ -> false },
        ): Set<ProjectChange> {
            val changes = HashSet<ProjectChange>()
            fun check(path: String) {
                val stamp = current[path] ?: FileStamp.MISSING
                if (stamp != (previous[path] ?: FileStamp.MISSING) && !isOwn(path, stamp)) areaOf(path)?.let(changes::add)
            }
            current.keys.forEach(::check)
            previous.keys.forEach { if (it !in current) check(it) }
            return changes
        }

        internal fun areaOf(path: String) = when {
            path.startsWith(".kubriko/actors/") || path.startsWith(".kubriko/managers/") -> ProjectChange.BLOCK_TYPE_DEFINITIONS
            ResourceScanner.classify(path) == ResourceGroup.SCENES -> ProjectChange.SCENES
            path.startsWith("src/") || "/src/" in path -> ProjectChange.SOURCES
            else -> null
        }
    }
}
