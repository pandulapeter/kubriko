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

import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.testFixtures.CountingActor
import com.pandulapeter.kubriko.testFixtures.awaitCondition
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals

class RemovedActorUpdateTest {

    @Test
    fun noUpdateAfterOnRemoved() = repeat(50) {
        assertNoUpdateAfterOnRemoved(ActorManager.newInstance())
    }

    @Test
    fun noUpdateAfterOnRemovedWithoutSleeping() = repeat(50) {
        assertNoUpdateAfterOnRemoved(ActorManager.newInstance(shouldPutFarAwayActorsToSleep = false))
    }

    @Test
    fun newActorIsUpdatedOnTheFirstTickAfterOnAdded() {
        val (kubriko, tickSource) = newTestKubriko()
        val a = CountingActor()
        kubriko.actorManager.add(a)
        awaitCondition { a in kubriko.actorManager.allActors.value }
        tickSource.tick(16)
        assertEquals(1, a.updates.get())
        kubriko.dispose()
    }

    private fun assertNoUpdateAfterOnRemoved(actorManager: ActorManager) {
        val (kubriko, tickSource) = newTestKubriko(actorManager = actorManager)
        val wasRemoved = AtomicBoolean(false)
        val actor = CountingActor(onRemovedAction = { wasRemoved.set(true) })
        kubriko.actorManager.add(actor)
        kubriko.actorManager.awaitProcessed()
        awaitCondition {
            tickSource.tick(16)
            actor.updates.get() >= 1
        }
        kubriko.actorManager.remove(actor)
        awaitCondition { wasRemoved.get() }
        val updates = actor.updates.get()
        repeat(5) { tickSource.tick(16) }
        assertEquals(updates, actor.updates.get())
        kubriko.dispose()
    }
}
