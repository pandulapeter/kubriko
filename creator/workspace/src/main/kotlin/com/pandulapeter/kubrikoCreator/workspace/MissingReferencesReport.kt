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
import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.context.HealthFinding
import com.pandulapeter.kubrikoCreator.context.missingReferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Says in the console what starts naming something that no longer exists, [DEBOUNCE_IN_MILLISECONDS] after the last
 * change: a type or Scene deleted outside Kubriko Creator (in a terminal, by a git checkout), whose Blocks a delete in
 * Kubriko Creator would have cleaned up. What already dangled when the project opened is left to the health checks.
 *
 * @param changes Emits whenever what names what may have changed.
 * @param compute Finds what names something that no longer exists; it runs on [computeDispatcher].
 * @param report Writes a line to the console.
 * @param reportFailure Says that a check failed; the next change checks again.
 */
internal class MissingReferencesReport(
    changes: Flow<Any?>,
    private val compute: suspend () -> List<HealthFinding>,
    scope: CoroutineScope,
    private val report: (ConsoleEntry) -> Unit,
    private val reportFailure: (Throwable) -> Unit,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    init {
        scope.launch {
            var known: Set<HealthFinding>? = null
            changes.onStart { emit(Unit) }.collectLatest {
                if (known != null) delay(DEBOUNCE_IN_MILLISECONDS)
                val current = try {
                    withContext(computeDispatcher) { compute() }.toSet()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    reportFailure(exception)
                    return@collectLatest
                }
                known?.let { previous ->
                    (current - previous).forEach { finding ->
                        report(ConsoleEntry.Status(WorkspaceStatus.REFERENCE_MISSING, isError = true, arguments = listOf(finding.detail.orEmpty(), finding.subject.toString())))
                    }
                }
                known = current
            }
        }
    }

    companion object {
        const val DEBOUNCE_IN_MILLISECONDS = 500L

        /**
         * The report of a workspace, whose references may change with its types and resources.
         */
        fun of(blockTypes: BlockTypesController?, resources: StateFlow<*>, commands: CommandExecutor, scope: CoroutineScope, report: (ConsoleEntry) -> Unit, reportFailure: (Throwable) -> Unit) = MissingReferencesReport(
            changes = merge(blockTypes?.types?.drop(1) ?: emptyFlow(), resources.drop(1)),
            compute = { commands.context.missingReferences() },
            scope = scope,
            report = report,
            reportFailure = reportFailure,
        )
    }
}
