/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko

import com.pandulapeter.kubriko.helpers.TickSource
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
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
    fun disposedSharedManagerIsNotUpdated() {
        val shared = CountingManager()
        val t1 = TickSource.manual()
        val t2 = TickSource.manual()
        val k1 = Kubriko.newInstance(shared, tickSource = t1)
        val k2 = Kubriko.newInstance(shared, tickSource = t2)
        t1.start()
        t2.start()
        t2.tick(16)
        assertEquals(1, shared.updates)
        k1.dispose()
        assertEquals(1, shared.disposes)
        t2.tick(16)
        assertEquals(1, shared.updates)
        k2.dispose()
    }

    @Test
    fun sharedManagerIsBoundToTheFirstStartedInstance() {
        val shared = StateManagerExposingManager()
        val t1 = TickSource.manual()
        val t2 = TickSource.manual()
        val k1 = Kubriko.newInstance(shared, tickSource = t1)
        val k2 = Kubriko.newInstance(shared, tickSource = t2)
        t2.start()
        t1.start()
        assertSame(k2.get<StateManager>(), shared.stateManager)
        k1.dispose()
        k2.dispose()
    }
}
