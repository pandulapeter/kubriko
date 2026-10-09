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
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.manager.ActorManager
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Checks that the shared test helpers keep the guarantees the other tests rely on.
 */
class TestFixturesTest {

    @Volatile
    private var sink: ByteArray? = null

    @Test
    fun awaitProcessedAppliesEarlierOperations() = repeat(200) {
        newManualKubriko().use { instance ->
            val removed = CountingActor()
            val kept = CountingActor()
            instance.actorManager.add(removed, kept)
            instance.actorManager.remove(removed)

            instance.actorManager.awaitProcessed()

            assertEquals(listOf<Actor>(kept), instance.actorManager.allActors.value.toList())
        }
    }

    @Test
    fun tickEmitsTheRequestedNumberOfTicks() {
        newManualKubriko().use { instance ->
            val actor = CountingActor()
            instance.actorManager.add(actor)
            instance.actorManager.awaitProcessed()

            instance.tick(16, count = 3)

            assertEquals(3, actor.updates.get())
        }
    }

    @Test
    fun tickUntilFailsOnTimeout() {
        newManualKubriko().use { instance ->
            assertFails { instance.tickUntil(timeoutInMilliseconds = 50) { false } }
        }
    }

    @Test
    fun closingAManualKubrikoDisposesIt() {
        val instance = newManualKubriko()

        instance.close()

        assertFailsWith<IllegalStateException> { instance.kubriko.get<ActorManager>() }
    }

    @Test
    fun awaitConditionFailsOnTimeout() {
        assertFails { awaitCondition(timeoutInMilliseconds = 20) { false } }
    }

    @Test
    fun blockerForcesOneBatch() {
        newManualKubriko().use { instance ->
            val blocker = Blocker()
            instance.actorManager.add(blocker)
            assertTrue(blocker.entered.await(5, TimeUnit.SECONDS))
            val actors = List(3) { CountingActor() }
            val sawAnotherPublished = AtomicBoolean(false)
            actors.forEach { actor ->
                actor.onAddedAction = {
                    if (instance.actorManager.allActors.value.any { it in actors }) {
                        sawAnotherPublished.set(true)
                    }
                }
            }

            actors.forEach { instance.actorManager.add(it) }
            blocker.release.countDown()
            instance.actorManager.awaitProcessed()

            actors.forEach { assertEquals(1, it.added.get()) }
            assertFalse(sawAnotherPublished.get())
        }
    }

    @Test
    fun recordingUncaughtExceptionsRecordsThemAndRestoresTheHandler() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        val failure = IllegalStateException()

        val recordedFailures = recordingUncaughtExceptions { recorded ->
            thread { throw failure }.join()
            recorded.toList()
        }

        assertEquals(listOf<Throwable>(failure), recordedFailures)
        assertSame(previousHandler, Thread.getDefaultUncaughtExceptionHandler())
    }

    @Test
    fun allocationMeasurementSeesAnAllocation() {
        assertTrue(measureAllocatedBytesPerRun { sink = ByteArray(1024) } >= 1024)
    }
}
