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
import com.pandulapeter.kubrikoCreator.actors.store.BlockTypesController
import com.pandulapeter.kubrikoCreator.commands.CommandExecutor
import com.pandulapeter.kubrikoCreator.context.HealthFinding
import com.pandulapeter.kubrikoCreator.context.PageSummary
import com.pandulapeter.kubrikoCreator.context.ProjectDigest
import com.pandulapeter.kubrikoCreator.context.digestAndHealthFindings
import com.pandulapeter.kubrikoCreator.context.pageSummaries
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneEditingController
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * What the AI panel's Game map shows.
 */
public data class GameMap(
    public val digest: ProjectDigest,
    public val findings: List<HealthFinding>,
    public val pages: List<PageSummary> = emptyList(),
)

/**
 * The Game map of the open project, computed off the main thread [DEBOUNCE_IN_MILLISECONDS] after the last change, and only
 * while something collects [gameMap].
 *
 * @param changes Emits whenever the map may have changed, and never a current value (a StateFlow's first is dropped).
 * @param compute Builds the map; it runs on [computeDispatcher], so it reads only what is safe off the main thread.
 * @param report Says that a computation failed; the previous map stays, and the next change computes again.
 */
public class GameMapState internal constructor(
    changes: Flow<Any?>,
    private val compute: suspend () -> GameMap,
    scope: CoroutineScope,
    private val report: (Throwable) -> Unit,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    public val gameMap: StateFlow<GameMap?> = flow {
        var isFirst = true
        // A newer change cancels the delayed or running computation, which is the debounce; the first one isn't delayed, so
        // opening the Game map shows it at once.
        emitAll(
            changes.onStart { emit(Unit) }.mapLatest {
                if (!isFirst) delay(DEBOUNCE_IN_MILLISECONDS)
                isFirst = false
                computeReporting()
            }.filterNotNull(),
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 0), null)

    private suspend fun computeReporting(): GameMap? = try {
        withContext(computeDispatcher) { compute() }
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        report(exception)
        null
    }

    internal companion object {
        const val DEBOUNCE_IN_MILLISECONDS = 250L

        /**
         * The Game map of a workspace, which may change with its types, resources, Scene descriptions, undo history, AI
         * steps, shown Scene, pages, and documents.
         */
        fun of(
            blockTypes: BlockTypesController?,
            resources: StateFlow<*>,
            scenes: SceneStore,
            commands: CommandExecutor,
            sceneEditing: SceneEditingController,
            scope: CoroutineScope,
            report: (Throwable) -> Unit,
        ) = GameMapState(
            changes = merge(
                blockTypes?.types?.drop(1) ?: emptyFlow(),
                resources.drop(1),
                scenes.descriptions.current.drop(1),
                commands.history.drop(1),
                commands.aiSteps.drop(1),
                sceneEditing.state.drop(1),
                commands.context.screenplay.revision.drop(1),
                commands.context.documents.revision.drop(1),
            ),
            compute = {
                val (digest, findings) = commands.context.digestAndHealthFindings()
                GameMap(digest, findings, withContext(commands.context.ioDispatcher) { commands.context.pageSummaries() })
            },
            scope = scope,
            report = report,
        )
    }
}
