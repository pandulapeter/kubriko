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

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.helpers.TickSource
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.testFixtures.CountingActor
import com.pandulapeter.kubriko.testFixtures.awaitCondition
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ActorProcessingStartTest {

    private class RecordingManager : Manager() {
        @Volatile
        var initialized = false

        override fun onInitialize(kubriko: Kubriko) {
            initialized = true
        }
    }

    @Test
    fun initialActorsWaitForStart() {
        val recordedInitialization = AtomicReference<Boolean?>(null)
        val actor = object : Actor {
            override fun onAdded(kubriko: Kubriko) {
                recordedInitialization.set(kubriko.get<RecordingManager>().initialized)
            }
        }
        val tickSource = TickSource.manual()
        val kubriko = Kubriko.newInstance(
            ActorManager.newInstance(initialActors = listOf(actor)),
            RecordingManager(),
            tickSource = tickSource,
        )
        val actorManager = kubriko.get<ActorManager>()
        Thread.sleep(100)
        assertTrue(actorManager.allActors.value.isEmpty())
        assertNull(recordedInitialization.get())
        tickSource.start()
        assertTrue(actor in actorManager.allActors.value)
        assertEquals(true, recordedInitialization.get())
        kubriko.dispose()
    }

    @Test
    fun callbackOperationsDuringTheDrainAreAppliedToo() = repeat(200) {
        val child = CountingActor()
        val parent = object : Actor {
            override fun onAdded(kubriko: Kubriko) = kubriko.get<ActorManager>().add(child)
        }
        val tickSource = TickSource.manual()
        val kubriko = Kubriko.newInstance(
            ActorManager.newInstance(initialActors = listOf(parent)),
            tickSource = tickSource,
        )
        tickSource.start()
        assertTrue(child in kubriko.get<ActorManager>().allActors.value)
        assertEquals(1, child.added.get())
        kubriko.dispose()
    }

    @Test
    fun firstTickAfterStartUpdatesInitialActors() = repeat(200) {
        val actor = CountingActor()
        val tickSource = TickSource.manual()
        val kubriko = Kubriko.newInstance(
            ActorManager.newInstance(initialActors = listOf(actor)),
            tickSource = tickSource,
        )
        tickSource.start()
        tickSource.tick(16)
        assertEquals(1, actor.updates.get())
        kubriko.dispose()
    }

    @Test
    fun preStartCallbacksRunOnTheStartingThread() {
        val actor = CountingActor()
        val tickSource = TickSource.manual()
        val kubriko = Kubriko.newInstance(
            ActorManager.newInstance(initialActors = listOf(actor)),
            tickSource = tickSource,
        )
        tickSource.start()
        assertSame(Thread.currentThread(), actor.onAddedThread.get())
        kubriko.dispose()
    }

    @Test
    fun addBeforeStartIsAppliedInOrderAfterStart() {
        val tickSource = TickSource.manual()
        val kubriko = Kubriko.newInstance(tickSource = tickSource)
        val actorManager = kubriko.get<ActorManager>()
        val a = CountingActor()
        val b = CountingActor()
        actorManager.add(a)
        actorManager.remove(a)
        actorManager.add(b)
        tickSource.start()
        assertTrue(b in actorManager.allActors.value)
        assertFalse(a in actorManager.allActors.value)
        kubriko.dispose()
    }

    @Test
    fun addAfterStartStaysAsynchronous() {
        val tickSource = TickSource.manual()
        val kubriko = Kubriko.newInstance(tickSource = tickSource)
        val actorManager = kubriko.get<ActorManager>()
        tickSource.start()
        val c = CountingActor()
        actorManager.add(c)
        awaitCondition { c in actorManager.allActors.value }
        assertNotSame(Thread.currentThread(), c.onAddedThread.get())
        kubriko.dispose()
    }
}
