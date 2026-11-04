/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import com.pandulapeter.kubrikoCreator.gradle.ProjectInspectionException
import com.pandulapeter.kubrikoCreator.project.ProjectProblem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * The game's module: the desktop module Kubriko Creator builds, loads the Scene editor's classes from, and plays, chosen
 * among every module with a `run` task when there are several (a game beside its own tools). The choice is this
 * computer's, kept in `.kubriko/local/module.json`; without one, or with one that is gone, the first module in Gradle's
 * order is the game's.
 *
 * The modules are asked from Gradle in the background as soon as the workspace opens, since only builds need them, and
 * asked again, once they are next needed, after a lookup that failed (a build file fixed since, or Gradle's download back
 * online) and after the build files changed ([onBuildFilesChanged]).
 *
 * @param findDesktopModulePaths Asks Gradle for the paths of the modules with a `run` task, in its order.
 * @param onChosen Called on the main thread once another module is chosen, whose classes are then the project's.
 */
public class GameModuleChoice internal constructor(
    rootDirectory: File,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val findDesktopModulePaths: suspend () -> List<String>,
    private val onChosen: () -> Unit,
) {
    private val store = LocalProjectStore(rootDirectory, FILE_NAME, LocalModuleSettings.serializer())
    private val saves = Channel<String>(Channel.CONFLATED)
    private val _modules = MutableStateFlow<GameModules?>(null)

    /**
     * The modules the game could be and the one it is, or null until Gradle has named them.
     */
    public val modules: StateFlow<GameModules?> = _modules.asStateFlow()
    private val lookup = AtomicReference(lookUp())

    init {
        lookup.get().start()
        scope.launch(ioDispatcher) {
            for (path in saves) store.write(LocalModuleSettings(module = path))
        }
    }

    private fun lookUp(): Deferred<GameModules> = scope.async(ioDispatcher, start = CoroutineStart.LAZY) {
        val paths = findDesktopModulePaths()
        val saved = store.read()?.module
        val found = GameModules(available = paths, chosen = saved?.takeIf { it in paths } ?: paths.firstOrNull() ?: throw ProjectInspectionException(ProjectProblem.NO_DESKTOP_MODULE))
        // A lookup replaced while it ran read the build files as they were before, so only the latest one is shown.
        if (lookup.get() === coroutineContext.job) {
            _modules.update { shown -> shown?.chosen?.takeIf { it in paths }?.let { found.copy(chosen = it) } ?: found }
        }
        found
    }

    /**
     * The Gradle path of the game's module, waiting for Gradle to name the modules if it hasn't yet.
     */
    internal suspend fun await(): String {
        val current = lookup.get()
        val found = try {
            current.await()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            // Lazily, so the next lookup reads the files as they are when it is needed, and only once.
            replace(current)
            throw exception
        }
        return (_modules.value ?: found).chosen
    }

    /**
     * Makes [path], one of [modules]' available ones, the game's module, and keeps it.
     */
    public fun choose(path: String) {
        val previous = _modules.getAndUpdate { modules -> modules?.takeIf { path in it.available }?.copy(chosen = path) ?: modules }
        if (previous == null || previous.chosen == path || path !in previous.available) return
        saves.trySend(path)
        onChosen()
    }

    /**
     * Notes that the build files changed, which may have added or removed modules, so the next build asks Gradle again.
     */
    internal fun onBuildFilesChanged() = replace(lookup.get())

    private fun replace(current: Deferred<GameModules>) {
        val next = lookUp()
        when {
            !lookup.compareAndSet(current, next) -> next.cancel()
            // One nothing awaited never started, and would otherwise wait in the workspace's scope until it closes.
            !current.isActive && !current.isCompleted -> current.cancel()
        }
    }

    internal companion object {
        const val FILE_NAME = "module.json"
    }
}

/**
 * The game's module chosen on this computer, by its Gradle path.
 */
@Serializable
private data class LocalModuleSettings(
    @SerialName("module") val module: String? = null,
)
