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

import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.KubrikoImpl
import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.newTestKubriko
import com.pandulapeter.kubriko.testFixtures.CountingActor
import com.pandulapeter.kubriko.testFixtures.ManualKubriko
import com.pandulapeter.kubriko.testFixtures.awaitCondition
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import com.pandulapeter.kubriko.types.SceneOffset
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActorManagerUpdateTest {

    private class FarAwayActor : CountingActor(), Positionable {
        override val body = PointBody(initialPosition = SceneOffset(100_000f.sceneUnit, 100_000f.sceneUnit))
    }

    private fun ManualKubriko.addAndAwait(actor: CountingActor) {
        actorManager.add(actor)
        actorManager.awaitProcessed()
    }

    @Test
    fun addedActorIsUpdatedOnEveryTickWithItsDelta() {
        newManualKubriko().use { instance ->
            val deltas = mutableListOf<Int>()
            val actor = object : CountingActor() {
                override fun update(deltaTimeInMilliseconds: Int) {
                    deltas.add(deltaTimeInMilliseconds)
                }
            }
            instance.addAndAwait(actor)

            instance.tick(16)
            instance.tick(20)

            assertEquals(listOf(16, 20), deltas)
        }
    }

    @Test
    fun actorIsUpdatedOnTheFirstTickAfterItIsPublished() {
        val (kubriko, tickSource) = newTestKubriko()
        try {
            val actor = CountingActor()
            kubriko.actorManager.add(actor)
            awaitCondition { actor in kubriko.actorManager.allActors.value }

            tickSource.tick(16)

            assertEquals(1, actor.updates.get())
        } finally {
            kubriko.dispose()
        }
    }

    @Test
    fun removedActorIsNotUpdatedAfterOnRemoved() = repeat(50) {
        assertNoUpdateAfterOnRemoved(ActorManager.newInstance())
    }

    @Test
    fun removedActorIsNotUpdatedAfterOnRemovedWithoutSleeping() = repeat(50) {
        assertNoUpdateAfterOnRemoved(ActorManager.newInstance(shouldPutFarAwayActorsToSleep = false))
    }

    @Test
    fun everyDynamicActorIsActiveAndUpdatedWithoutAViewport() {
        newManualKubriko().use { instance ->
            val actor = FarAwayActor()
            instance.addAndAwait(actor)

            instance.tick()

            assertEquals(1, actor.updates.get())
            assertTrue(actor in instance.actorManager.activeDynamicActors.value)
            assertTrue(instance.actorManager.visibleActorsWithinViewport.value.isEmpty())
        }
    }

    @Test
    fun farAwayActorsFallAsleepOnceTheViewportHasASize() {
        newManualKubriko().use { instance ->
            val actor = FarAwayActor()
            instance.addAndAwait(actor)
            instance.tick()

            (instance.kubriko as KubrikoImpl).viewportManager.updateSize(Size(1920f, 1080f))
            instance.tick(count = 2)
            val updates = actor.updates.get()
            instance.tick()

            assertFalse(actor in instance.actorManager.activeDynamicActors.value)
            assertEquals(updates, actor.updates.get())
        }
    }

    @Test
    fun pausedInstanceDoesNotUpdateActors() {
        newManualKubriko().use { instance ->
            val actor = CountingActor()
            instance.addAndAwait(actor)

            instance.kubriko.get<StateManager>().updateIsRunning(false)
            instance.tick()

            assertEquals(0, actor.updates.get())
        }
    }

    @Test
    fun shouldUpdateActorsWhileNotRunningKeepsUpdatingActorsWhilePaused() {
        newManualKubriko(ActorManager.newInstance(shouldUpdateActorsWhileNotRunning = true)).use { instance ->
            val actor = CountingActor()
            instance.addAndAwait(actor)

            instance.kubriko.get<StateManager>().updateIsRunning(false)
            instance.tick()

            assertEquals(1, actor.updates.get())
        }
    }

    private fun assertNoUpdateAfterOnRemoved(actorManager: ActorManager) {
        val (kubriko, tickSource) = newTestKubriko(actorManager = actorManager)
        try {
            val wasRemoved = AtomicBoolean(false)
            val actor = CountingActor(onRemovedAction = { wasRemoved.set(true) })
            kubriko.actorManager.add(actor)
            kubriko.actorManager.awaitProcessed()
            tickSource.tick(16)

            kubriko.actorManager.remove(actor)
            awaitCondition { wasRemoved.get() }
            val updates = actor.updates.get()
            repeat(5) { tickSource.tick(16) }

            assertEquals(updates, actor.updates.get())
        } finally {
            kubriko.dispose()
        }
    }
}
