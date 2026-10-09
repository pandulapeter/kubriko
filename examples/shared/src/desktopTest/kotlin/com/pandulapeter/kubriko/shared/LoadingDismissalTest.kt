/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shared

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class LoadingDismissalTest {

    @Test
    fun onLoadedRunsOnlyOnceTheDelayHasPassedAfterAnAcceptedValue() = runTest {
        val values = MutableStateFlow(emptyList<Int>())
        var dismissalCount = 0
        values.dismissLoadingWhen(backgroundScope, isLoaded = { it.isNotEmpty() }) { dismissalCount++ }
        advanceTimeBy(LoadingDismissalDelay * 2)
        assertEquals(0, dismissalCount)
        values.value = listOf(1)
        runCurrent()
        advanceTimeBy(LoadingDismissalDelay - 1.milliseconds)
        assertEquals(0, dismissalCount)
        advanceTimeBy(1.milliseconds)
        runCurrent()
        assertEquals(1, dismissalCount)
    }

    @Test
    fun aNewLoadAfterARejectedValueDismissesAgain() = runTest {
        val values = MutableStateFlow(listOf(1))
        var dismissalCount = 0
        values.dismissLoadingWhen(backgroundScope, isLoaded = { it.isNotEmpty() }) { dismissalCount++ }
        runCurrent()
        advanceTimeBy(LoadingDismissalDelay)
        runCurrent()
        values.value = emptyList()
        runCurrent()
        values.value = listOf(2)
        runCurrent()
        advanceTimeBy(LoadingDismissalDelay)
        runCurrent()
        assertEquals(2, dismissalCount)
    }

    @Test
    fun theSameValueReturningAfterARejectedOneIsNotANewLoad() = runTest {
        val values = MutableStateFlow(listOf(1))
        var dismissalCount = 0
        values.dismissLoadingWhen(backgroundScope, isLoaded = { it.isNotEmpty() }) { dismissalCount++ }
        runCurrent()
        advanceTimeBy(LoadingDismissalDelay)
        runCurrent()
        values.value = emptyList()
        runCurrent()
        values.value = listOf(1)
        runCurrent()
        advanceTimeBy(LoadingDismissalDelay)
        runCurrent()
        assertEquals(1, dismissalCount)
    }
}
