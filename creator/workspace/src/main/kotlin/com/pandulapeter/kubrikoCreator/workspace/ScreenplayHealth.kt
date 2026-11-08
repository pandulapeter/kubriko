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
import com.pandulapeter.kubrikoCreator.context.screenplayFindings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.merge

/**
 * What about the open project's screenplay is worth a look ([screenplayFindings]), for the sidebar to show beside what it
 * is about: worked out off the main thread a moment after the last change, and only while something collects [findings].
 *
 * @param changes Emits whenever the findings may have changed, and never a current value.
 * @param report Says that working them out failed; the previous ones stay.
 */
public class ScreenplayHealth internal constructor(
    changes: Flow<Any?>,
    compute: suspend () -> List<HealthFinding>,
    scope: CoroutineScope,
    report: (Throwable) -> Unit,
    computeDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /**
     * The findings as last worked out, or null before the first time.
     */
    public val findings: StateFlow<List<HealthFinding>?> = debouncedComputation(changes, compute, scope, report, computeDispatcher, DEBOUNCE_IN_MILLISECONDS)

    internal companion object {
        const val DEBOUNCE_IN_MILLISECONDS = 500L

        /**
         * The screenplay's findings of a workspace, which may change with its types, resources, pages, documents, and undo
         * history.
         */
        fun of(blockTypes: BlockTypesController?, resources: StateFlow<*>, commands: CommandExecutor, scope: CoroutineScope, report: (Throwable) -> Unit) = ScreenplayHealth(
            changes = merge(
                blockTypes?.types?.drop(1) ?: emptyFlow(),
                resources.drop(1),
                commands.context.screenplay.revision.drop(1),
                commands.context.documents.revision.drop(1),
                commands.history.drop(1),
            ),
            compute = { commands.context.screenplayFindings() },
            scope = scope,
            report = report,
        )
    }
}
