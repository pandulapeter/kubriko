/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testFixtures

import com.pandulapeter.kubriko.actor.Actor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestFixturesTest {

    @Volatile
    private var sink: ByteArray? = null

    @Test
    fun awaitProcessedAppliesEarlierOperations() = repeat(200) {
        val instance = newManualKubriko()
        val a = CountingActor()
        val b = CountingActor()
        instance.actorManager.add(a, b)
        instance.actorManager.remove(a)
        instance.actorManager.awaitProcessed()
        assertEquals(listOf<Actor>(b), instance.actorManager.allActors.value.toList())
        instance.dispose()
    }

    @Test
    fun tickUntilFailsOnTimeout() {
        val instance = newManualKubriko()
        assertFails { instance.tickUntil(timeoutInMilliseconds = 50) { false } }
        instance.dispose()
    }

    @Test
    fun blockerForcesOneBatch() {
        val instance = newManualKubriko()
        val blocker = Blocker()
        instance.actorManager.add(blocker)
        assertTrue(blocker.entered.await(5, TimeUnit.SECONDS))
        val actors = List(3) { CountingActor() }
        val sawAnother = AtomicBoolean(false)
        actors.forEach { actor ->
            actor.onAddedAction = {
                if (instance.actorManager.allActors.value.any { it in actors }) {
                    sawAnother.set(true)
                }
            }
        }
        actors.forEach { instance.actorManager.add(it) }
        blocker.release.countDown()
        instance.actorManager.awaitProcessed()
        actors.forEach { assertEquals(1, it.added.get()) }
        assertFalse(sawAnother.get())
        instance.dispose()
    }

    @Test
    fun allocationMeasurementSeesAnAllocation() {
        assertTrue(measureAllocatedBytesPerRun { sink = ByteArray(1024) } >= 1024)
    }
}
