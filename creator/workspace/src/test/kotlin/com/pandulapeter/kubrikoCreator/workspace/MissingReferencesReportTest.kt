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

import com.pandulapeter.kubrikoCreator.console.ConsoleEntry
import com.pandulapeter.kubrikoCreator.context.HealthFinding
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MissingReferencesReportTest {
    private val changes = MutableSharedFlow<Unit>()
    private val console = mutableListOf<ConsoleEntry>()
    private val failures = mutableListOf<Throwable>()
    private var findings = listOf(missing("Gone", "Door#0.0"))
    private var isFailing = false

    private fun missing(name: String, place: String) = HealthFinding(HealthFinding.Kind.REFERENCE_MISSING, RelationSubject.Type(name), place)

    private fun TestScope.report() = MissingReferencesReport(
        changes = changes,
        compute = {
            check(!isFailing) { "boom" }
            findings
        },
        scope = CoroutineScope(backgroundScope.coroutineContext + StandardTestDispatcher(testScheduler)),
        report = { console += it },
        reportFailure = { failures += it },
        computeDispatcher = StandardTestDispatcher(testScheduler),
    )

    private suspend fun TestScope.change() {
        changes.emit(Unit)
        advanceTimeBy(MissingReferencesReport.DEBOUNCE_IN_MILLISECONDS + 1)
        runCurrent()
    }

    @Test
    fun onlyWhatStartsToDangleAfterOpeningIsReportedOnce() = runTest {
        report()
        runCurrent()
        assertEquals(emptyList(), console)
        findings = findings + missing("Brick", "Ball#1.2")
        change()
        change()
        assertEquals(listOf<ConsoleEntry>(ConsoleEntry.Status(WorkspaceStatus.REFERENCE_MISSING, isError = true, arguments = listOf("Ball#1.2", "Brick"))), console)
    }

    @Test
    fun aFailedCheckIsReportedAndTheNextChangeChecksAgain() = runTest {
        report()
        runCurrent()
        isFailing = true
        findings = findings + missing("Brick", "Ball#1.2")
        change()
        assertEquals(1, failures.size)
        isFailing = false
        change()
        assertEquals(1, console.size)
    }
}
