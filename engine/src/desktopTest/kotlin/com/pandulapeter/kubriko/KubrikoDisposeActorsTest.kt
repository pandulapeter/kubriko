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

import com.pandulapeter.kubriko.testFixtures.CountingActor
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class KubrikoDisposeActorsTest {

    private val instance = newManualKubriko()

    @AfterTest
    fun disposeInstance() = instance.dispose()

    @Test
    fun disposeReachesLiveActorsWithoutRemovingThem() {
        val actors = List(10) { CountingActor() }
        instance.actorManager.add(actors)
        instance.actorManager.awaitProcessed()

        instance.dispose()

        actors.forEach {
            assertEquals(1, it.disposed.get())
            assertEquals(0, it.removed.get())
        }
    }

    @Test
    fun actorRemovedEarlierIsNotDisposedAgain() {
        val actor = CountingActor()
        instance.actorManager.add(actor)
        instance.actorManager.awaitProcessed()
        instance.actorManager.remove(actor)
        instance.actorManager.awaitProcessed()

        instance.dispose()

        assertEquals(1, actor.disposed.get())
    }

    @Test
    fun disposeRunsOnTheDisposingThread() {
        val actor = CountingActor()
        instance.actorManager.add(actor)
        instance.actorManager.awaitProcessed()

        val disposingThread = thread { instance.dispose() }
        disposingThread.join()

        assertSame(disposingThread, actor.disposeThread.get())
    }

    @Test
    fun throwingDisposeDoesNotStopTheOthers() {
        val thrower = CountingActor(disposeAction = { throw IllegalStateException("dispose failed") })
        val others = List(3) { CountingActor() }
        instance.actorManager.add(listOf(thrower) + others)
        instance.actorManager.awaitProcessed()

        instance.dispose()

        others.forEach { assertEquals(1, it.disposed.get()) }
    }
}
