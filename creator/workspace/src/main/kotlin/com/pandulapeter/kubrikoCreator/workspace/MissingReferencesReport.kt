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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Says in the console what starts naming something that no longer exists after a change outside Kubriko Creator (a type
 * or Scene deleted in a terminal, or by a git checkout), whose Blocks a delete in Kubriko Creator would have cleaned up.
 * It checks [DEBOUNCE_IN_MILLISECONDS] after every change, but reports only within [REPORT_WINDOW_IN_MILLISECONDS] of one
 * from outside ([onChangedOutside]), long enough for the types it changed to be read again: the types loading at opening,
 * and the user's own edits (a placeholder typed halfway), are left to the health checks and the Block cards.
 *
 * @param changes Emits whenever what names what may have changed.
 * @param compute Finds what names something that no longer exists; it runs on [computeDispatcher].
 * @param report Writes a line to the console.
 * @param reportFailure Says that a check failed; the next change checks again.
 * @param now The time in milliseconds, which tests replace with virtual time.
 */
internal class MissingReferencesReport(
    changes: Flow<Any?>,
    private val compute: suspend () -> List<HealthFinding>,
    scope: CoroutineScope,
    private val report: (ConsoleEntry) -> Unit,
    private val reportFailure: (Throwable) -> Unit,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val outsideChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var reportsUntil = Long.MIN_VALUE

    init {
        scope.launch {
            var known: Set<HealthFinding>? = null
            merge(changes, outsideChanges).onStart { emit(Unit) }.collectLatest {
                if (known != null) delay(DEBOUNCE_IN_MILLISECONDS)
                val current = try {
                    withContext(computeDispatcher) { compute() }.toSet()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    reportFailure(exception)
                    return@collectLatest
                }
                known?.takeIf { now() <= reportsUntil }?.let { previous ->
                    (current - previous).forEach { finding ->
                        report(ConsoleEntry.Status(WorkspaceStatus.REFERENCE_MISSING, isError = true, arguments = listOf(finding.detail.orEmpty(), finding.usage.orEmpty())))
                    }
                }
                known = current
            }
        }
    }

    /**
     * Notes that files changed outside Kubriko Creator, so that what starts dangling within the window is reported.
     */
    fun onChangedOutside() {
        reportsUntil = now() + REPORT_WINDOW_IN_MILLISECONDS
        outsideChanges.tryEmit(Unit)
    }

    companion object {
        const val DEBOUNCE_IN_MILLISECONDS = 500L
        const val REPORT_WINDOW_IN_MILLISECONDS = 5_000L

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
