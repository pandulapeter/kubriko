/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sceneRuntime
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.foundation.LatestRequests
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import com.pandulapeter.kubrikoCreator.session.ProjectClasspath
import com.pandulapeter.kubrikoCreator.session.SessionInitScript
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Opens Scenes in Kubriko Creator's own window: compiles the project, loads its classes next to Creator's, and hands
 * the project's entry point and the Scene's Actors to the Scene editor. A project on another Kubriko version, or on a newer
 * Compose or Kotlin, can't share Creator's classes, so its Scenes can't be edited ([SceneEditingState.Unsupported]).
 *
 * @param runtime What the project's classes are loaded next to: Creator's Kubriko and library versions, and the loader
 *   ([SceneRuntimeSetup]).
 * @param classes How the project's classes are built for the editor ([SceneClassesBuild]).
 * @param sources What the open Scene is made of, read from the project as it is now ([SceneSources]).
 * @param releaseScope Where released classes are closed, a few seconds later: one that outlives [scope], which closing the
 *   workspace cancels first, so that they are closed even then.
 */
public class SceneEditingController(
    private val runtime: SceneRuntimeSetup,
    private val classes: SceneClassesBuild,
    private val sources: SceneSources,
    private val scope: CoroutineScope,
    private val releaseScope: CoroutineScope = scope,
    private val ioDispatcher: CoroutineDispatcher,
    private val report: (ConsoleEntry) -> Unit,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private val _state = MutableStateFlow<SceneEditingState>(SceneEditingState.Closed)
    /**
     * The Scene being edited, if any, and how far it got.
     */
    public val state: StateFlow<SceneEditingState> = _state.asStateFlow()
    private var job: Job? = null
    private var updateJob: Job? = null

    /**
     * The project's classes for its current sources, being built and loaded or loaded already, kept for every Scene
     * opened until something changes ([markOutdated]).
     */
    private var loading: Deferred<LoadResult>? = null

    /**
     * The classes the open Scene is drawn with, which are kept until it shows others, even once [loading] has newer ones.
     */
    private var shown: Deferred<LoadResult>? = null

    /**
     * The looks of the Actor types that the classes of [shown] were built from.
     */
    private var shownLooks: Map<String, ActorTypeLook> = emptyMap()
    private var isLoadingOutdated = false
    private var revision = 0
    private val sceneReads = LatestRequests()
    private var lastReportedNewer: List<SharedLibraries.NewerLibrary> = emptyList()

    /**
     * Builds and loads the project's classes in the background, so that the first Scene opened doesn't wait for a build,
     * and then warms up the compiler, so that the first change doesn't wait for it either.
     */
    public fun prepare() {
        if (loading != null) return
        val prepared = startLoading()
        scope.launch { if (prepared.await() is LoadResult.Loaded) classes.warmUpCompiler() }
    }

    /**
     * Shows [scene], built and loaded with the project's classes, or with the classes loaded for the Scene shown before
     * when nothing changed since ([markOutdated]), so that going back to a Scene doesn't build again.
     */
    public fun open(scene: ProjectResource): Unit = open(scene, isRebuilding = false)

    private fun open(scene: ProjectResource, isRebuilding: Boolean) {
        close()
        val startTime = currentTimeMillis()
        _state.value = SceneEditingState.Preparing(scene)
        val loading = loading?.takeUnless { isRebuilding || isLoadingOutdated } ?: startLoading()
        job = scope.launch {
            val read = async(ioDispatcher) { sources.readScene(scene) to sources.readSceneProblem(scene) }
            _state.value = when (val result = loading.await()) {
                is LoadResult.Loaded -> SceneEditingState.Open(
                    scene = scene,
                    entryPoint = show(loading, result),
                    actors = read.await().first,
                    revision = ++revision,
                    previews = currentPreviews(),
                    problem = read.await().second,
                ).also {
                    report(ConsoleEntry.Status(SceneStatus.OPENED, arguments = listOf(((currentTimeMillis() - startTime) / 100 / 10f).toString())))
                }
                is LoadResult.Failed -> SceneEditingState.Failed(scene, result.reason).also { forget(loading) }
                is LoadResult.Unsupported -> SceneEditingState.Unsupported(scene, result.reason, result.arguments).also { forget(loading) }
            }
            if (isLoadingOutdated) update()
        }
    }

    /**
     * Starts building and loading the classes of the current sources, releasing the ones loaded before unless they are shown.
     */
    private fun startLoading() = scope.async { buildAndLoad() }.also {
        loading?.takeIf { previous -> previous !== shown }?.let(::release)
        loading = it
        isLoadingOutdated = false
    }

    /**
     * Notes that the open Scene is drawn with [loaded], the result of [deferred], releasing the classes it showed before.
     */
    private fun show(deferred: Deferred<LoadResult>, loaded: LoadResult.Loaded): SpriteLoadingEntryPoint {
        shown?.takeIf { it !== deferred && it !== loading }?.let(::release)
        shown = deferred
        shownLooks = loaded.looks
        return SpriteLoadingEntryPoint(loaded.loaded.entryPoint, ProjectSpriteLoader(sources.spriteFiles()))
    }

    /**
     * Builds the open Scene's classes again in the background and swaps them in, keeping the old ones on screen until
     * then, and building again after a build during which something changed. A failed build leaves the old classes and
     * shows that the Scene is outdated.
     */
    private fun update() {
        if (updateJob?.isActive == true) return
        updateJob = scope.launch {
            while (isLoadingOutdated && _state.value is SceneEditingState.Open) {
                _state.update { if (it is SceneEditingState.Open) it.copy(isUpdating = true) else it }
                val deferred = startLoading()
                val result = deferred.await()
                val open = _state.value as? SceneEditingState.Open ?: break
                if (result is LoadResult.Loaded) {
                    _state.value = open.copy(
                        entryPoint = show(deferred, result),
                        revision = ++revision,
                        previews = currentPreviews(),
                        isOutdated = false,
                        isUpdating = isLoadingOutdated,
                    )
                } else {
                    forget(deferred)
                    _state.value = open.copy(isOutdated = true, isUpdating = false)
                }
            }
        }
    }

    /**
     * Notes that the project's code threw while the view of [revision]'s classes was being created: the Scene can't be
     * shown with them, or, while a view of older classes [isShowingOlderClasses], stays on those as outdated.
     */
    public fun onViewFailed(revision: Int, exception: Throwable, isShowingOlderClasses: Boolean) {
        val open = _state.value as? SceneEditingState.Open ?: return
        if (open.revision != revision) return
        val problem = exception.toString()
        report(ConsoleEntry.Status(SceneProblem.VIEW_FAILED, isError = true, arguments = listOf(problem)))
        _state.value = if (isShowingOlderClasses) open.copy(isOutdated = true) else SceneEditingState.Failed(open.scene, SceneProblem.VIEW_FAILED, listOf(problem))
    }

    private fun currentPreviews() = ActorTypeLook.previewsOf(sources.actorTypeLooks(), shownLooks)

    /**
     * Notes that the Actor types changed in memory, which the open Scene previews right away, before they are written.
     */
    public fun onActorTypesChanged(): Unit = _state.update { if (it is SceneEditingState.Open) it.copy(previews = currentPreviews()) else it }

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
        classes.awaitWrites()
        val looks = sources.actorTypeLooks()
        val isBuilt = classes.runTask(
            SessionInitScript.CLASSPATH_TASK_NAME,
            mapOf("kubrikoCreatorMode" to "classpath", "kubrikoCreatorClasspathFile" to classes.classpathFile.absolutePath),
        )
        val classpath = if (isBuilt) withContext(ioDispatcher) { readClasspath(classes.classpathFile) } else null
        return when {
            classpath == null -> LoadResult.Failed(SceneProblem.BUILD_FAILED)
            classpath.kubrikoVersion != runtime.creatorKubrikoVersion -> LoadResult.Unsupported(SceneProblem.UNSUPPORTED, listOf(runtime.creatorKubrikoVersion, classpath.kubrikoVersion))
            else -> when (val verdict = SharedLibraries.check(classpath.libraries, runtime.creatorLibraries)) {
                is SharedLibraries.Verdict.Refused -> LoadResult.Unsupported(
                    SceneProblem.UNSUPPORTED_LIBRARY,
                    verdict.library.let { listOf(it.module, it.projectVersion, it.creatorVersion) },
                )
                else -> {
                    reportNewer(verdict)
                    load(classpath, looks)
                }
            }
        }
    }

    private suspend fun load(classpath: ProjectClasspath, looks: Map<String, ActorTypeLook>) = try {
        LoadResult.Loaded(createDisposingIfCancelled(ioDispatcher, create = { runtime.entryPointLoader.load(classpath) }, dispose = { it.close() }), looks)
    } catch (exception: EntryPointLoadingException) {
        if (exception.cause != null) classes.requestCleanBuild()
        report(ConsoleEntry.Status(exception.reason, isError = true))
        LoadResult.Failed(exception.reason)
    }

    /**
     * Says which shared libraries are newer in the project, once for each set of them, as every change builds again.
     */
    private fun reportNewer(verdict: SharedLibraries.Verdict) {
        val newer = (verdict as? SharedLibraries.Verdict.Newer)?.libraries.orEmpty()
        if (newer == lastReportedNewer) return
        lastReportedNewer = newer
        if (newer.isNotEmpty()) {
            val list = newer.joinToString(", ") { "${it.module} ${it.projectVersion} (Creator: ${it.creatorVersion})" }
            report(ConsoleEntry.Status(SceneStatus.NEWER_LIBRARIES, arguments = listOf(list)))
        }
    }

    /**
     * Opens the current Scene again, with the project's classes compiled from scratch and loaded anew.
     */
    public fun reload() {
        if (_state.value != SceneEditingState.Closed) classes.requestCleanBuild()
        when (val current = _state.value) {
            is SceneEditingState.Open -> open(current.scene, isRebuilding = true)
            is SceneEditingState.Preparing -> open(current.scene, isRebuilding = true)
            is SceneEditingState.Unsupported -> open(current.scene, isRebuilding = true)
            is SceneEditingState.Failed -> open(current.scene, isRebuilding = true)
            SceneEditingState.Closed -> Unit
        }
    }

    /**
     * Notes that an Actor type or a source changed: the open Scene is built again in the background and swaps its
     * classes when that is done ([update]), and the next Scene opened builds the project again.
     */
    public fun markOutdated() {
        isLoadingOutdated = true
        if (_state.value is SceneEditingState.Open) update()
    }

    /**
     * Notes that commands changed the Scene files called [names]: the open Scene shows its new Actors if it is one of
     * them, and is closed if it was deleted.
     */
    public fun onScenesChanged(names: Collection<String>) {
        val open = _state.value as? SceneEditingState.Open ?: return
        when {
            open.sceneName !in names -> Unit
            open.scene.file.isFile -> {
                val request = sceneReads.start()
                scope.launch {
                    val (actors, problem) = withContext(ioDispatcher) { sources.readScene(open.scene) to sources.readSceneProblem(open.scene) }
                    if (sceneReads.isLatest(request)) {
                        _state.update { if (it is SceneEditingState.Open && it.scene == open.scene) it.copy(actors = actors, problem = problem) else it }
                    }
                }
            }
            else -> close()
        }
    }

    /**
     * Shows the open Scene again (or closes it if it is gone) after Scene files changed outside Kubriko Creator, which
     * doesn't say which ones.
     */
    public fun onScenesChangedOnDisk() {
        val open = _state.value as? SceneEditingState.Open ?: return
        onScenesChanged(listOf(open.sceneName))
    }

    /**
     * Notes that a command moved the Scene [oldName] to [scene], which the open Scene follows if it was that one.
     */
    public fun onSceneRenamed(oldName: String, scene: ProjectResource): Unit = _state.update { state ->
        when {
            state is SceneEditingState.Open && state.sceneName == oldName -> state.copy(scene = scene)
            state is SceneEditingState.Preparing && SceneStore.nameOf(state.scene.file) == oldName -> state.copy(scene = scene)
            else -> state
        }
    }

    /**
     * Closes the Scene, keeping the project's classes loaded for the next one.
     */
    public fun close() {
        job?.cancel()
        job = null
        updateJob?.cancel()
        updateJob = null
        shown?.takeIf { it !== loading }?.let(::release)
        shown = null
        _state.value = SceneEditingState.Closed
    }

    /**
     * Closes the Scene and releases the project's classes, when the workspace closes.
     */
    public fun dispose() {
        close()
        loading?.let(::release)
        loading = null
    }

    private fun release(released: Deferred<LoadResult>) {
        released.cancel()
        releaseScope.launch {
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
    /**
     * @param looks The looks of the Actor types when the build started, which the classes draw.
     */
    data class Loaded(
        val loaded: LoadedEntryPoint,
        val looks: Map<String, ActorTypeLook>,
    ) : LoadResult

    data class Failed(val reason: SceneProblem) : LoadResult

    data class Unsupported(val reason: SceneProblem, val arguments: List<String>) : LoadResult
}
