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
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.newTestKubriko
import com.pandulapeter.kubriko.testFixtures.Blocker
import com.pandulapeter.kubriko.testFixtures.CountingActor
import com.pandulapeter.kubriko.testFixtures.awaitCondition
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BatchCallbackPairingTest {

    private fun KubrikoImpl.inOneBatch(operations: () -> Unit) {
        val blocker = Blocker()
        actorManager.add(blocker)
        assertTrue(blocker.entered.await(5, TimeUnit.SECONDS))
        operations()
        blocker.release.countDown()
    }

    @Test
    fun addThenRemoveInOneBatchGetsBothCallbacks() {
        val (kubriko, _) = newTestKubriko()
        val x = CountingActor()
        kubriko.inOneBatch {
            kubriko.actorManager.add(x)
            kubriko.actorManager.remove(x)
        }
        awaitCondition { x.removed.get() == 1 }
        assertEquals(1, x.added.get())
        assertEquals(1, x.disposed.get())
        assertEquals(1, x.removed.get())
        assertFalse(x in kubriko.actorManager.allActors.value)
        kubriko.dispose()
    }

    @Test
    fun removeThenAddInOneBatchGetsNoCallbacks() {
        val (kubriko, _) = newTestKubriko()
        val x = CountingActor()
        kubriko.actorManager.add(x)
        kubriko.actorManager.awaitProcessed()
        kubriko.inOneBatch {
            kubriko.actorManager.remove(x)
            kubriko.actorManager.add(x)
        }
        kubriko.actorManager.awaitProcessed()
        assertEquals(1, x.added.get())
        assertEquals(0, x.removed.get())
        assertTrue(x in kubriko.actorManager.allActors.value)
        kubriko.dispose()
    }

    @Test
    fun removeAllThenAddKeepsTheActorWithoutCallbacks() {
        val (kubriko, _) = newTestKubriko()
        val x = CountingActor()
        kubriko.actorManager.add(x)
        kubriko.actorManager.awaitProcessed()
        kubriko.inOneBatch {
            kubriko.actorManager.removeAll()
            kubriko.actorManager.add(x)
        }
        awaitCondition { kubriko.actorManager.allActors.value.toList() == listOf<Actor>(x) }
        assertEquals(1, x.added.get())
        assertEquals(0, x.removed.get())
        kubriko.dispose()
    }
}
