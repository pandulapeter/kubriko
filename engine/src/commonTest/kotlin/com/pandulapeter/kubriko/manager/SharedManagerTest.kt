/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.manager

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.helpers.TickSource
import com.pandulapeter.kubriko.helpers.extensions.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SharedManagerTest {

    private class CountingManager : Manager() {
        var updates = 0
        var disposes = 0

        override fun onUpdate(deltaTimeInMilliseconds: Int) {
            updates++
        }

        override fun onDispose() {
            disposes++
        }
    }

    private class StateManagerExposingManager : Manager() {
        val stateManager by manager<StateManager>()
    }

    @Test
    fun sharedManagerIsUpdatedByEveryInstance() {
        val shared = CountingManager()
        val firstTickSource = TickSource.manual()
        val secondTickSource = TickSource.manual()
        val first = Kubriko.newInstance(shared, tickSource = firstTickSource)
        val second = Kubriko.newInstance(shared, tickSource = secondTickSource)
        try {
            firstTickSource.start()
            secondTickSource.start()

            firstTickSource.tick(16)
            secondTickSource.tick(16)

            assertEquals(2, shared.updates)
        } finally {
            first.dispose()
            second.dispose()
        }
    }

    @Test
    fun sharedManagerIsDisposedWithTheFirstDisposedInstanceAndNoLongerUpdated() {
        val shared = CountingManager()
        val firstTickSource = TickSource.manual()
        val secondTickSource = TickSource.manual()
        val first = Kubriko.newInstance(shared, tickSource = firstTickSource)
        val second = Kubriko.newInstance(shared, tickSource = secondTickSource)
        try {
            firstTickSource.start()
            secondTickSource.start()

            first.dispose()
            secondTickSource.tick(16)

            assertEquals(1, shared.disposes)
            assertEquals(0, shared.updates)
        } finally {
            first.dispose()
            second.dispose()
        }
    }

    @Test
    fun sharedManagerIsBoundToTheFirstStartedInstance() {
        val shared = StateManagerExposingManager()
        val firstTickSource = TickSource.manual()
        val secondTickSource = TickSource.manual()
        val first = Kubriko.newInstance(shared, tickSource = firstTickSource)
        val second = Kubriko.newInstance(shared, tickSource = secondTickSource)
        try {
            secondTickSource.start()
            firstTickSource.start()

            assertSame(second.get<StateManager>(), shared.stateManager)
        } finally {
            first.dispose()
            second.dispose()
        }
    }
}
