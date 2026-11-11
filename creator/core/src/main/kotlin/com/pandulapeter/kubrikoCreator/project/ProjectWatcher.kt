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

import com.pandulapeter.kubrikoCreator.actors.store.BlockTypeStore
import com.pandulapeter.kubrikoCreator.foundation.ATOMIC_WRITE_SUFFIX
import com.pandulapeter.kubrikoCreator.screenplay.PageNaming
import com.pandulapeter.kubrikoCreator.screenplay.ScreenplaySettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
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
public class ProjectWatcher(
    private val rootDirectory: File,
    private val hiddenDirectory: String?,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val intervalInMilliseconds: Long = 1_000,
    private val ownWrites: OwnWrites = OwnWrites(),
) {

    /**
     * Starts watching, calling [onChange] with the areas that changed and the snapshot that found them.
     */
    public fun start(onChange: (changes: Set<ProjectChange>, snapshot: ProjectSnapshot) -> Unit): AutoCloseable {
        val job = scope.launch {
            var previous: ProjectSnapshot? = null
            while (isActive) {
                try {
                    val current = withContext(ioDispatcher) { snapshot(rootDirectory, hiddenDirectory, ownWrites.generation) }
                    val last = previous
                    previous = current
                    if (last != null) {
                        val changes = changedAreas(last, current) { path, stamp -> ownWrites.isOwn(File(rootDirectory, path), stamp) }
                        if (changes.isNotEmpty()) onChange(changes, current)
                    }
                } catch (exception: Exception) {
                    if (exception is CancellationException) throw exception
                    coroutineContext[CoroutineExceptionHandler]?.handleException(coroutineContext, exception)
                }
                delay(intervalInMilliseconds)
            }
        }
        return AutoCloseable { job.cancel() }
    }

    public companion object {
        /**
         * The stamp of every file in an area or in the resource tree, by its path relative to [rootDirectory], read after
         * Creator's own writes reached [ownWriteGeneration].
         */
        public fun snapshot(rootDirectory: File, hiddenDirectory: String?, ownWriteGeneration: Long = 0): ProjectSnapshot {
            val stamps = HashMap<String, FileStamp>()
            ProjectTree.walk(rootDirectory, hiddenDirectory = hiddenDirectory)
                .filter { it.isFile }
                .forEach { file ->
                    val path = file.relativeTo(rootDirectory).invariantSeparatorsPath
                    if (areaOf(path) != null || ResourceScanner.classify(path) != null) stamps[path] = FileStamp(file.length(), file.lastModified())
                }
            return ProjectSnapshot(stamps, ownWriteGeneration)
        }

        /**
         * The areas with a file that was added, removed, resized, or modified between [previous] and [current], other
         * than those [isOwn] says Kubriko Creator changed itself.
         */
        internal fun changedAreas(
            previous: ProjectSnapshot,
            current: ProjectSnapshot,
            isOwn: (path: String, stamp: FileStamp) -> Boolean = { _, _ -> false },
        ): Set<ProjectChange> {
            val changes = HashSet<ProjectChange>()
            fun check(path: String) {
                val stamp = current.stamps[path] ?: FileStamp.MISSING
                if (stamp != (previous.stamps[path] ?: FileStamp.MISSING) && !isOwn(path, stamp)) areaOf(path)?.let(changes::add)
            }
            current.stamps.keys.forEach(::check)
            previous.stamps.keys.forEach { if (it !in current.stamps) check(it) }
            return changes
        }

        /**
         * The root project's settings files, whose `include` lines make modules too.
         */
        private val SETTINGS_FILES = setOf("settings.gradle.kts", "settings.gradle")

        internal fun areaOf(path: String) = when {
            path.endsWith(ATOMIC_WRITE_SUFFIX) -> null
            path == BlockTypeStore.SETTINGS_FILE -> ProjectChange.SETTINGS
            path == GradlePropertiesFile.NAME -> ProjectChange.GAME_SETTINGS
            path.startsWith(".kubriko/actors/") || path.startsWith(".kubriko/managers/") -> ProjectChange.BLOCK_TYPE_DEFINITIONS
            ResourceScanner.classify(path) == ResourceGroup.SCENES -> ProjectChange.SCENES
            path.startsWith(PageNaming.FOLDER + "/") || path == ScreenplaySettings.FILE -> ProjectChange.SCREENPLAY
            ResourceScanner.isDocument(path) -> ProjectChange.DOCUMENTS
            ResourceScanner.isBuildFile(path) || path in SETTINGS_FILES || path == VersionCatalog.FILE -> ProjectChange.MODULES
            path.startsWith("src/") || "/src/" in path -> ProjectChange.SOURCES
            else -> null
        }
    }
}
