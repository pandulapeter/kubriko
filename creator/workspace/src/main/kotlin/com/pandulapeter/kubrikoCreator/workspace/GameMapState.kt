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
import com.pandulapeter.kubrikoCreator.context.pageSummaries
import com.pandulapeter.kubrikoCreator.context.projectDigest
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.merge
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
 * @param compute Builds the map without its findings; it runs on [computeDispatcher], so it reads only what is safe off
 *   the main thread.
 * @param findings The health checks' findings, worked out once for the map and the editors ([ProjectHealth]).
 * @param report Says that a computation failed; the previous map stays, and the next change computes again.
 */
public class GameMapState internal constructor(
    changes: Flow<Any?>,
    compute: suspend () -> GameMap,
    findings: StateFlow<List<HealthFinding>?>,
    scope: CoroutineScope,
    report: (Throwable) -> Unit,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    public val gameMap: StateFlow<GameMap?> = combine(
        debouncedComputation(changes, compute, scope, report, computeDispatcher, DEBOUNCE_IN_MILLISECONDS),
        findings,
    ) { map, current -> map?.copy(findings = current.orEmpty()) }.stateIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 0), null)

    internal companion object {
        const val DEBOUNCE_IN_MILLISECONDS = 250L

        /**
         * The Game map of a workspace, which may change with its types, resources, Scene descriptions, undo history, AI
         * steps, pages, documents, and files changed outside Kubriko Creator ([changedOutside]), with [findings].
         */
        fun of(
            blockTypes: BlockTypesController?,
            resources: StateFlow<*>,
            scenes: SceneStore,
            commands: CommandExecutor,
            changedOutside: Flow<Unit>,
            findings: StateFlow<List<HealthFinding>?>,
            scope: CoroutineScope,
            report: (Throwable) -> Unit,
        ) = GameMapState(
            changes = merge(
                blockTypes?.types?.drop(1) ?: emptyFlow(),
                resources.drop(1),
                scenes.descriptions.current.drop(1),
                commands.history.drop(1),
                commands.aiSteps.drop(1),
                changedOutside,
                commands.context.screenplay.revision.drop(1),
                commands.context.documents.revision.drop(1),
            ),
            compute = { GameMap(commands.context.projectDigest(), emptyList(), withContext(commands.context.ioDispatcher) { commands.context.pageSummaries() }) },
            findings = findings,
            scope = scope,
            report = report,
        )
    }
}
