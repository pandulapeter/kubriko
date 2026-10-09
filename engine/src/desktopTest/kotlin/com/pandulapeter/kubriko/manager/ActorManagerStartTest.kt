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
import com.pandulapeter.kubriko.KubrikoImpl
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.helpers.TickSource
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.testFixtures.CountingActor
import com.pandulapeter.kubriko.testFixtures.awaitCondition
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ActorManagerStartTest {

    private class InitializationRecordingManager : Manager() {
        @Volatile
        var isInitializedByOnInitialize = false

        override fun onInitialize(kubriko: Kubriko) {
            isInitializedByOnInitialize = true
        }
    }

    @Test
    fun initialActorsAreAddedOnStartWithEveryManagerInitialized() {
        val sawInitializedManager = AtomicReference<Boolean?>(null)
        val actor = object : Actor {
            override fun onAdded(kubriko: Kubriko) {
                sawInitializedManager.set(kubriko.get<InitializationRecordingManager>().isInitializedByOnInitialize)
            }
        }
        val tickSource = TickSource.manual()
        val scheduler = TestCoroutineScheduler()
        // A test dispatcher, so that idling it proves nothing is applied in the background before start().
        val kubriko = KubrikoImpl(
            ActorManager.newInstance(initialActors = listOf(actor)),
            InitializationRecordingManager(),
            tickSource = tickSource,
            isLoggingEnabled = false,
            instanceNameForLogging = null,
            dispatcher = StandardTestDispatcher(scheduler),
        )
        try {
            val actorManager = kubriko.get<ActorManager>()
            scheduler.advanceUntilIdle()
            assertTrue(actorManager.allActors.value.isEmpty())
            assertNull(sawInitializedManager.get())

            tickSource.start()

            assertTrue(actor in actorManager.allActors.value)
            assertEquals(true, sawInitializedManager.get())
        } finally {
            kubriko.dispose()
        }
    }

    @Test
    fun operationsIssuedBeforeStartAreAppliedInIssueOrderByTheTimeStartReturns() {
        newManualKubriko(shouldStart = false).use { instance ->
            val removedAgain = CountingActor()
            val kept = CountingActor()
            instance.actorManager.add(removedAgain, kept)
            instance.actorManager.remove(removedAgain)
            val addedLast = CountingActor()
            instance.actorManager.add(addedLast)

            instance.tickSource.start()

            assertEquals(listOf<Actor>(kept, addedLast), instance.actorManager.allActors.value.toList())
            assertEquals(1, removedAgain.added.get())
            assertEquals(1, removedAgain.removed.get())
        }
    }

    @Test
    fun operationsIssuedBeforeStartRunTheirCallbacksOnTheStartingThread() {
        newManualKubriko(shouldStart = false).use { instance ->
            val kept = CountingActor()
            val removed = CountingActor()
            instance.actorManager.add(kept, removed)
            instance.actorManager.remove(removed)

            instance.tickSource.start()

            assertSame(Thread.currentThread(), kept.onAddedThread.get())
            assertSame(Thread.currentThread(), removed.disposeThread.get())
            assertSame(Thread.currentThread(), removed.onRemovedThread.get())
        }
    }

    @Test
    fun operationsIssuedFromCallbacksDuringStartAreAppliedBeforeStartReturns() = repeat(200) {
        val child = CountingActor()
        val parent = object : Actor {
            override fun onAdded(kubriko: Kubriko) = kubriko.get<ActorManager>().add(child)
        }
        newManualKubriko(ActorManager.newInstance(initialActors = listOf(parent)), shouldStart = false).use { instance ->
            instance.tickSource.start()

            assertTrue(child in instance.actorManager.allActors.value)
            assertEquals(1, child.added.get())
        }
    }

    @Test
    fun firstTickAfterStartUpdatesInitialActors() = repeat(200) {
        val actor = CountingActor()
        newManualKubriko(ActorManager.newInstance(initialActors = listOf(actor))).use { instance ->
            instance.tick()

            assertEquals(1, actor.updates.get())
        }
    }

    @Test
    fun operationsIssuedAfterStartRunTheirCallbacksOnABackgroundThread() {
        newManualKubriko().use { instance ->
            val actor = CountingActor()

            instance.actorManager.add(actor)
            awaitCondition { actor in instance.actorManager.allActors.value }

            assertNotSame(Thread.currentThread(), actor.onAddedThread.get())
        }
    }
}
