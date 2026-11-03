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

import com.pandulapeter.kubrikoCreator.context.ProjectDigest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameMapStateTest {
    private val changes = MutableSharedFlow<Unit>()
    private val reports = mutableListOf<Throwable>()
    private var computeCount = 0
    private var computeGate: CompletableDeferred<Unit>? = null
    private var failingComputation = 0

    private fun mapNamed(name: String) = GameMap(ProjectDigest.of(name, emptyList(), emptyMap(), emptyMap()), emptyList())

    private fun TestScope.gameMapState(
        changes: Flow<Any?> = this@GameMapStateTest.changes,
        scopeDispatcher: CoroutineDispatcher = StandardTestDispatcher(testScheduler),
    ): GameMapState {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return GameMapState(
            changes = changes,
            compute = {
                val count = ++computeCount
                if (count == 2) computeGate?.await()
                check(count != failingComputation) { "boom" }
                mapNamed("map $count")
            },
            scope = CoroutineScope(backgroundScope.coroutineContext + scopeDispatcher),
            report = { reports += it },
            computeDispatcher = dispatcher,
        )
    }

    private fun TestScope.collect(state: GameMapState) = backgroundScope.launch(StandardTestDispatcher(testScheduler)) { state.gameMap.collect {} }

    private suspend fun TestScope.changeAndWaitOutTheDebounce() {
        changes.emit(Unit)
        advanceTimeBy(GameMapState.DEBOUNCE_IN_MILLISECONDS + 1)
    }

    @Test
    fun nothingIsComputedWhileNothingCollects() = runTest {
        val state = gameMapState()
        runCurrent()
        changes.emit(Unit)
        advanceTimeBy(1_000)
        assertEquals(0, computeCount)
        assertNull(state.gameMap.value)
    }

    @Test
    fun theFirstMapComesAtOnceAndQuickChangesComputeOnce() = runTest {
        val state = gameMapState()
        collect(state)
        runCurrent()
        assertEquals(1, computeCount)
        assertEquals(mapNamed("map 1"), state.gameMap.value)
        changes.emit(Unit)
        advanceTimeBy(50)
        changes.emit(Unit)
        advanceTimeBy(GameMapState.DEBOUNCE_IN_MILLISECONDS - 1)
        assertEquals(1, computeCount)
        advanceTimeBy(2)
        assertEquals(2, computeCount)
        assertEquals(mapNamed("map 2"), state.gameMap.value)
    }

    @Test
    fun aSlowComputationIsCancelledByANewerChange() = runTest {
        val gate = CompletableDeferred<Unit>().also { computeGate = it }
        val state = gameMapState()
        collect(state)
        runCurrent()
        changeAndWaitOutTheDebounce()
        assertEquals(2, computeCount)
        changeAndWaitOutTheDebounce()
        assertEquals(mapNamed("map 3"), state.gameMap.value)
        gate.complete(Unit)
        runCurrent()
        assertEquals(mapNamed("map 3"), state.gameMap.value)
    }

    @Test
    fun aFailedComputationIsReportedAndTheNextChangeComputesAgain() = runTest {
        failingComputation = 2
        val state = gameMapState()
        collect(state)
        runCurrent()
        changeAndWaitOutTheDebounce()
        assertEquals(listOf("boom"), reports.map { it.message })
        assertEquals(mapNamed("map 1"), state.gameMap.value)
        changeAndWaitOutTheDebounce()
        assertEquals(mapNamed("map 3"), state.gameMap.value)
        assertEquals(1, reports.size)
    }

    @Test
    fun stoppingCollectingStopsComputing() = runTest {
        val state = gameMapState()
        val collector = collect(state)
        runCurrent()
        collector.cancel()
        runCurrent()
        changes.emit(Unit)
        advanceTimeBy(1_000)
        assertEquals(1, computeCount)
    }

    @Test
    fun stateFlowSourcesDontDelayTheFirstMap() = runTest {
        val types = MutableStateFlow(0)
        val resources = MutableStateFlow(0)
        // Unconfined, so the first computation has started by the time the sources would replay their values.
        val state = gameMapState(changes = merge(types.drop(1), resources.drop(1)), scopeDispatcher = UnconfinedTestDispatcher(testScheduler))
        collect(state)
        runCurrent()
        assertEquals(0L, testScheduler.currentTime)
        assertEquals(mapNamed("map 1"), state.gameMap.value)
    }
}
