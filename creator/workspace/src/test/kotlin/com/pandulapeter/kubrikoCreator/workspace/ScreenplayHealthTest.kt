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

import com.pandulapeter.kubrikoCreator.context.HealthFinding
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenplayHealthTest {

    @Test
    fun theFindingsAreWorkedOutWhileShownAndAgainAMomentAfterAChange() = runTest {
        val changes = MutableSharedFlow<Unit>()
        var count = 0
        val health = ScreenplayHealth(
            changes = changes,
            compute = { List(++count) { HealthFinding(HealthFinding.Kind.UNMENTIONED, RelationSubject.Type("Ball")) } },
            scope = CoroutineScope(backgroundScope.coroutineContext + StandardTestDispatcher(testScheduler)),
            report = {},
            computeDispatcher = StandardTestDispatcher(testScheduler),
        )
        runCurrent()
        assertEquals(0, count)
        backgroundScope.launch(StandardTestDispatcher(testScheduler)) { health.findings.collect {} }
        runCurrent()
        assertEquals(1, health.findings.value?.size)
        changes.emit(Unit)
        changes.emit(Unit)
        advanceTimeBy(ScreenplayHealth.DEBOUNCE_IN_MILLISECONDS + 1)
        runCurrent()
        assertEquals(2, health.findings.value?.size)
    }
}
