/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import com.pandulapeter.kubrikoCreator.workspace.ConsoleEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.status_scene_in_separate_window
import kubriko.creator.generated.resources.status_scene_opened
import java.io.File
import java.io.IOException

/**
 * Opens Scenes in Kubriko Creator's own window: compiles the project, loads its classes next to Creator's, and hands
 * the project's entry point to the Scene editor. A project on another Kubriko version can't share Creator's classes,
 * so its Scenes open in a session window instead ([openInSessionWindow]).
 *
 * @param runTask Runs one of the init script's tasks with the given properties, reporting problems in the console.
 */
internal class SceneEditingController(
    private val creatorKubrikoVersion: String,
    private val runTask: suspend (taskName: String, properties: Map<String, String>) -> Boolean,
    private val entryPointLoader: EntryPointLoader,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val openInSessionWindow: (ProjectResource) -> Unit,
    private val report: (ConsoleEntry) -> Unit,
    private val createClasspathFile: () -> File = { File.createTempFile("kubriko-creator-classpath", ".txt").apply { deleteOnExit() } },
) {
    private val _state = MutableStateFlow<SceneEditingState>(SceneEditingState.Closed)
    val state = _state.asStateFlow()
    private var job: Job? = null
    private var loadedEntryPoint: LoadedEntryPoint? = null
    private var revision = 0

    fun open(scene: ProjectResource) {
        close()
        _state.value = SceneEditingState.Preparing(scene)
        job = scope.launch {
            val classpathFile = createClasspathFile()
            val isBuilt = runTask(
                SessionInitScript.CLASSPATH_TASK_NAME,
                mapOf("kubrikoCreatorMode" to "classpath", "kubrikoCreatorClasspathFile" to classpathFile.absolutePath),
            )
            val classpath = if (isBuilt) withContext(ioDispatcher) { readClasspath(classpathFile) } else null
            when {
                classpath == null -> _state.value = SceneEditingState.Closed
                classpath.kubrikoVersion != creatorKubrikoVersion -> {
                    _state.value = SceneEditingState.Closed
                    report(ConsoleEntry.Status(Res.string.status_scene_in_separate_window))
                    openInSessionWindow(scene)
                }
                else -> try {
                    val loaded = withContext(ioDispatcher) { entryPointLoader.load(classpath) }
                    loadedEntryPoint = loaded
                    _state.value = SceneEditingState.Open(scene = scene, entryPoint = loaded.entryPoint, revision = ++revision)
                    report(ConsoleEntry.Status(Res.string.status_scene_opened))
                } catch (exception: EntryPointLoadingException) {
                    _state.value = SceneEditingState.Closed
                    report(ConsoleEntry.Status(exception.reason, isError = true))
                }
            }
        }
    }

    /**
     * Opens the current Scene again, with the project's classes compiled and loaded anew.
     */
    fun reload() {
        when (val current = _state.value) {
            is SceneEditingState.Open -> open(current.scene)
            is SceneEditingState.Preparing -> open(current.scene)
            SceneEditingState.Closed -> Unit
        }
    }

    /**
     * Notes that an Actor type changed, so the open Scene editor shows classes that are no longer current.
     */
    fun markOutdated() = _state.update { if (it is SceneEditingState.Open) it.copy(isOutdated = true) else it }

    fun close() {
        job?.cancel()
        job = null
        _state.value = SceneEditingState.Closed
        loadedEntryPoint?.let { loaded ->
            // The editor leaves composition on the next frame, and may still load classes while it is torn down.
            scope.launch {
                delay(RELEASE_DELAY_IN_MILLISECONDS)
                loaded.close()
            }
        }
        loadedEntryPoint = null
    }

    private fun readClasspath(file: File) = try {
        ProjectClasspath.parse(file.readText())
    } catch (_: IOException) {
        null
    } finally {
        file.delete()
    }

    private companion object {
        const val RELEASE_DELAY_IN_MILLISECONDS = 5_000L
    }
}
