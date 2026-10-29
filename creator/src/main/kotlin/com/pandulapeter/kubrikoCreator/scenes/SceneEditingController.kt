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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.scene_build_failed
import kubriko.creator.generated.resources.status_scene_opened
import org.jetbrains.compose.resources.StringResource
import java.io.File
import java.io.IOException

/**
 * Opens Scenes in Kubriko Creator's own window: compiles the project, loads its classes next to Creator's, and hands
 * the project's entry point and the Scene's Actors to the Scene editor. A project on another Kubriko version can't
 * share Creator's classes, so its Scenes can't be edited ([SceneEditingState.Unsupported]).
 *
 * @param runTask Runs one of the init script's tasks with the given properties, reporting problems in the console.
 * @param readScene The Actors of a Scene file, empty if it can't be read. Called off the main thread, while the project
 *   builds, so that the [SceneStore] it reads from has the Scene parsed for the first command on it.
 * @param spriteFiles The project's sprite files by the name of their resource accessor, which the Scene editor reads
 *   itself ([SpriteLoadingEntryPoint]).
 * @param requestCleanBuild Makes the next build compile everything from scratch, for when the classes may not match the
 *   sources: on Reload, and when they couldn't be loaded.
 */
internal class SceneEditingController(
    private val creatorKubrikoVersion: String,
    private val runTask: suspend (taskName: String, properties: Map<String, String>) -> Boolean,
    private val entryPointLoader: EntryPointLoader,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val report: (ConsoleEntry) -> Unit,
    private val readScene: (ProjectResource) -> List<SceneActorEntry>,
    private val createClasspathFile: () -> File = { File.createTempFile("kubriko-creator-classpath", ".txt").apply { deleteOnExit() } },
    private val spriteFiles: () -> Map<String, File> = { emptyMap() },
    private val requestCleanBuild: () -> Unit = {},
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private val _state = MutableStateFlow<SceneEditingState>(SceneEditingState.Closed)
    val state = _state.asStateFlow()
    private var job: Job? = null

    /**
     * The project's classes for its current sources, being built and loaded or loaded already, kept for every Scene
     * opened until something changes ([markOutdated]).
     */
    private var loading: Deferred<LoadResult>? = null
    private var isLoadingOutdated = false
    private var revision = 0

    /**
     * Builds and loads the project's classes in the background, so that the first Scene opened doesn't wait for a build.
     */
    fun prepare() {
        if (loading == null) startLoading()
    }

    /**
     * Shows [scene], built and loaded with the project's classes, or with the classes loaded for the Scene shown before
     * when nothing changed since ([markOutdated]), so that going back to a Scene doesn't build again.
     */
    fun open(scene: ProjectResource) = open(scene, isRebuilding = false)

    private fun open(scene: ProjectResource, isRebuilding: Boolean) {
        close()
        if (isRebuilding || isLoadingOutdated) releaseLoading()
        val startTime = currentTimeMillis()
        _state.value = SceneEditingState.Preparing(scene)
        val loading = loading ?: startLoading()
        job = scope.launch {
            val actors = async(ioDispatcher) { readScene(scene) }
            _state.value = when (val result = loading.await()) {
                is LoadResult.Loaded -> SceneEditingState.Open(
                    scene = scene,
                    entryPoint = SpriteLoadingEntryPoint(result.loaded.entryPoint, ProjectSpriteLoader(spriteFiles())),
                    actors = actors.await(),
                    revision = ++revision,
                    isOutdated = isLoadingOutdated,
                ).also {
                    report(ConsoleEntry.Status(Res.string.status_scene_opened, arguments = listOf(((currentTimeMillis() - startTime) / 100 / 10f).toString())))
                }
                is LoadResult.Failed -> SceneEditingState.Failed(scene, result.reason).also { forget(loading) }
                is LoadResult.Unsupported -> SceneEditingState.Unsupported(scene, result.kubrikoVersion).also { forget(loading) }
            }
        }
    }

    private fun startLoading() = scope.async { buildAndLoad() }.also {
        loading = it
        isLoadingOutdated = false
    }

    /**
     * Drops a result that isn't a loaded entry point once a Scene showed it, so the next Scene builds again.
     */
    private fun forget(failed: Deferred<LoadResult>) {
        if (loading === failed) loading = null
    }

    /**
     * Builds the project and loads its classes, or says why it couldn't.
     */
    private suspend fun buildAndLoad(): LoadResult {
        val classpathFile = createClasspathFile()
        val isBuilt = runTask(
            SessionInitScript.CLASSPATH_TASK_NAME,
            mapOf("kubrikoCreatorMode" to "classpath", "kubrikoCreatorClasspathFile" to classpathFile.absolutePath),
        )
        val classpath = if (isBuilt) withContext(ioDispatcher) { readClasspath(classpathFile) } else null
        return when {
            classpath == null -> LoadResult.Failed(Res.string.scene_build_failed)
            classpath.kubrikoVersion != creatorKubrikoVersion -> LoadResult.Unsupported(classpath.kubrikoVersion)
            else -> try {
                LoadResult.Loaded(withContext(ioDispatcher) { entryPointLoader.load(classpath) })
            } catch (exception: EntryPointLoadingException) {
                if (exception.cause != null) requestCleanBuild()
                report(ConsoleEntry.Status(exception.reason, isError = true))
                LoadResult.Failed(exception.reason)
            }
        }
    }

    /**
     * Opens the current Scene again, with the project's classes compiled from scratch and loaded anew.
     */
    fun reload() {
        if (_state.value != SceneEditingState.Closed) requestCleanBuild()
        when (val current = _state.value) {
            is SceneEditingState.Open -> open(current.scene, isRebuilding = true)
            is SceneEditingState.Preparing -> open(current.scene, isRebuilding = true)
            is SceneEditingState.Unsupported -> open(current.scene, isRebuilding = true)
            is SceneEditingState.Failed -> open(current.scene, isRebuilding = true)
            SceneEditingState.Closed -> Unit
        }
    }

    /**
     * Notes that an Actor type or a source changed: the open Scene shows that its classes are no longer current, and
     * the next Scene opened builds the project again.
     */
    fun markOutdated() {
        isLoadingOutdated = true
        _state.update { if (it is SceneEditingState.Open) it.copy(isOutdated = true) else it }
    }

    /**
     * Notes that commands changed the Scene files called [names]: the open Scene shows its new Actors if it is one of
     * them, and is closed if it was deleted.
     */
    fun onScenesChanged(names: Collection<String>) {
        val open = _state.value as? SceneEditingState.Open ?: return
        when {
            open.sceneName !in names -> Unit
            open.scene.file.isFile -> scope.launch {
                val actors = withContext(ioDispatcher) { readScene(open.scene) }
                _state.update { if (it is SceneEditingState.Open && it.revision == open.revision) it.copy(actors = actors) else it }
            }
            else -> close()
        }
    }

    /**
     * Notes that a command moved the Scene [oldName] to [scene], which the open Scene follows if it was that one.
     */
    fun onSceneRenamed(oldName: String, scene: ProjectResource) = _state.update { state ->
        when {
            state is SceneEditingState.Open && state.sceneName == oldName -> state.copy(scene = scene)
            state is SceneEditingState.Preparing && SceneStore.nameOf(state.scene.file) == oldName -> state.copy(scene = scene)
            else -> state
        }
    }

    /**
     * Closes the Scene, keeping the project's classes loaded for the next one.
     */
    fun close() {
        job?.cancel()
        job = null
        _state.value = SceneEditingState.Closed
    }

    /**
     * Closes the Scene and releases the project's classes, when the workspace closes.
     */
    fun dispose() {
        close()
        releaseLoading()
    }

    private fun releaseLoading() {
        val released = loading ?: return
        loading = null
        released.cancel()
        scope.launch {
            val loaded = try {
                released.await() as? LoadResult.Loaded
            } catch (_: CancellationException) {
                null
            } ?: return@launch
            // The editor leaves composition on the next frame, and may still load classes while it is torn down.
            delay(RELEASE_DELAY_IN_MILLISECONDS)
            loaded.loaded.close()
        }
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

private sealed interface LoadResult {
    data class Loaded(val loaded: LoadedEntryPoint) : LoadResult

    data class Failed(val reason: StringResource) : LoadResult

    data class Unsupported(val kubrikoVersion: String) : LoadResult
}
