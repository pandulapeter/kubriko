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

import com.pandulapeter.kubriko.KubrikoImpl
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.testFixtures.ManualKubriko
import com.pandulapeter.kubriko.testFixtures.awaitCondition
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StateManagerTest {

    private class UpdateCountingManager : Manager() {
        var updates = 0

        override fun onUpdate(deltaTimeInMilliseconds: Int) {
            updates++
        }
    }

    private val ManualKubriko.stateManager get() = kubriko.get<StateManager>()

    /** Focus is only ever reported by a `KubrikoViewport`, which a unit test cannot compose. */
    private fun ManualKubriko.reportFocus(isFocused: Boolean) {
        (kubriko as KubrikoImpl).stateManager.updateFocus(isFocused)
        awaitCondition { stateManager.isFocused.value == isFocused }
    }

    @Test
    fun instanceStartsRunningWhenStartedByDefault() {
        newManualKubriko(shouldStart = false).use { instance ->
            assertFalse(instance.stateManager.isRunning.value)

            instance.tickSource.start()

            assertTrue(instance.stateManager.isRunning.value)
        }
    }

    @Test
    fun instanceWithoutAutoStartStaysPausedUntilResumed() {
        newManualKubriko(StateManager.newInstance(shouldAutoStart = false)).use { instance ->
            assertFalse(instance.stateManager.isRunning.value)

            instance.stateManager.updateIsRunning(true)

            assertTrue(instance.stateManager.isRunning.value)
        }
    }

    @Test
    fun updateIsRunningPausesAndResumes() {
        newManualKubriko().use { instance ->
            instance.stateManager.updateIsRunning(false)
            assertFalse(instance.stateManager.isRunning.value)

            instance.stateManager.updateIsRunning(true)
            assertTrue(instance.stateManager.isRunning.value)
        }
    }

    @Test
    fun instanceNeverShownInAViewportStaysFocused() {
        newManualKubriko().use { instance ->
            instance.tick(count = 3)

            assertTrue(instance.stateManager.isFocused.value)
        }
    }

    @Test
    fun unfocusedInstanceIsNotRunningEvenWhenResumed() {
        newManualKubriko().use { instance ->
            instance.reportFocus(false)

            instance.stateManager.updateIsRunning(true)

            assertFalse(instance.stateManager.isRunning.value)
        }
    }

    @Test
    fun regainingFocusResumesAnInstanceThatWasRunning() {
        newManualKubriko().use { instance ->
            instance.reportFocus(false)

            instance.reportFocus(true)

            assertTrue(instance.stateManager.isRunning.value)
        }
    }

    @Test
    fun managersKeepReceivingUpdatesWhilePaused() {
        val manager = UpdateCountingManager()
        newManualKubriko(manager).use { instance ->
            instance.stateManager.updateIsRunning(false)

            instance.tick(count = 2)

            assertEquals(2, manager.updates)
        }
    }

    @Test
    fun isRunningEmitsItsCurrentValueFirst() {
        newManualKubriko().use { instance ->
            instance.stateManager.updateIsRunning(false)

            val firstEmission = runBlocking { withTimeout(2_000) { instance.stateManager.isRunning.first() } }

            assertFalse(firstEmission)
        }
    }
}
