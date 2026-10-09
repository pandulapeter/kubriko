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

import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.newTestKubriko
import com.pandulapeter.kubriko.testFixtures.CountingActor
import com.pandulapeter.kubriko.testFixtures.awaitCondition
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DuplicateAdditionTest {

    private class CountingUnique : CountingActor(), Unique

    @Test
    fun addingTheSameActorTwiceKeepsOneCopyAndRemovesOnce() {
        val (kubriko, tickSource) = newTestKubriko()
        val actorManager = kubriko.actorManager
        val a = CountingActor()
        actorManager.add(a)
        actorManager.awaitProcessed()
        actorManager.add(a)
        actorManager.add(a, a)
        actorManager.awaitProcessed()
        assertEquals(1, actorManager.allActors.value.count { it === a })
        assertEquals(1, a.added.get())
        awaitCondition {
            tickSource.tick(16)
            a.updates.get() > 0
        }
        val before = a.updates.get()
        tickSource.tick(16)
        assertEquals(1, a.updates.get() - before)

        actorManager.remove(a)
        actorManager.awaitProcessed()
        assertFalse(a in actorManager.allActors.value)
        assertEquals(1, a.removed.get())
        kubriko.dispose()
    }

    @Test
    fun reAddingTheLiveUniqueInstanceIsANoOp() {
        val (kubriko, _) = newTestKubriko()
        val actorManager = kubriko.actorManager
        val u = CountingUnique()
        actorManager.add(u)
        actorManager.awaitProcessed()
        actorManager.add(u)
        actorManager.awaitProcessed()
        assertEquals(1, u.added.get())
        assertEquals(0, u.removed.get())
        assertTrue(u in actorManager.allActors.value)
        kubriko.dispose()
    }

    @Test
    fun replacingAUniqueWithAnotherInstanceStillWorks() {
        val (kubriko, _) = newTestKubriko()
        val actorManager = kubriko.actorManager
        val u1 = CountingUnique()
        val u2 = CountingUnique()
        actorManager.add(u1)
        actorManager.awaitProcessed()
        actorManager.add(u2)
        actorManager.awaitProcessed()
        assertTrue(u2 in actorManager.allActors.value)
        assertFalse(u1 in actorManager.allActors.value)
        assertEquals(1, u1.removed.get())
        assertEquals(1, u2.added.get())
        kubriko.dispose()
    }
}
