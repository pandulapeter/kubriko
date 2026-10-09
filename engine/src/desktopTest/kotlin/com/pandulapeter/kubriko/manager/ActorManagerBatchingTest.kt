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

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.testFixtures.Blocker
import com.pandulapeter.kubriko.testFixtures.CountingActor
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActorManagerBatchingTest {

    private val instance = newManualKubriko()
    private val actorManager = instance.actorManager

    @AfterTest
    fun disposeInstance() = instance.dispose()

    private fun inOneBatch(operations: () -> Unit) {
        val blocker = Blocker()
        actorManager.add(blocker)
        assertTrue(blocker.entered.await(5, TimeUnit.SECONDS))
        operations()
        blocker.release.countDown()
        actorManager.awaitProcessed()
    }

    @Test
    fun addThenRemoveInOneBatchPairsTheCallbacksWithoutEverPublishingTheActor() {
        val wasEverPublished = AtomicBoolean(false)
        lateinit var actor: CountingActor
        actor = CountingActor(onRemovedAction = { wasEverPublished.set(actor in actorManager.allActors.value) })

        inOneBatch {
            actorManager.add(actor)
            actorManager.remove(actor)
        }

        assertEquals(1, actor.added.get())
        assertEquals(1, actor.disposed.get())
        assertEquals(1, actor.removed.get())
        assertFalse(wasEverPublished.get())
        assertFalse(actor in actorManager.allActors.value)
    }

    @Test
    fun removeThenAddInOneBatchKeepsTheActorWithoutCallbacks() {
        val actor = CountingActor()
        actorManager.add(actor)
        actorManager.awaitProcessed()

        inOneBatch {
            actorManager.remove(actor)
            actorManager.add(actor)
        }

        assertEquals(1, actor.added.get())
        assertEquals(0, actor.removed.get())
        assertTrue(actor in actorManager.allActors.value)
    }

    @Test
    fun removeAllThenAddInOneBatchKeepsTheActorWithoutCallbacks() {
        val actor = CountingActor()
        actorManager.add(actor)
        actorManager.awaitProcessed()

        inOneBatch {
            actorManager.removeAll()
            actorManager.add(actor)
        }

        assertEquals(listOf<Actor>(actor), actorManager.allActors.value.toList())
        assertEquals(1, actor.added.get())
        assertEquals(0, actor.removed.get())
    }

    @Test
    fun operationsInOneBatchAreAppliedInIssueOrder() {
        val first = CountingActor()
        val second = CountingActor()
        val third = CountingActor()

        inOneBatch {
            actorManager.add(first, second)
            actorManager.remove(first)
            actorManager.add(third)
            actorManager.add(first)
        }

        assertEquals(listOf<Actor>(second, third, first), actorManager.allActors.value.filterNot { it is Blocker })
    }
}
