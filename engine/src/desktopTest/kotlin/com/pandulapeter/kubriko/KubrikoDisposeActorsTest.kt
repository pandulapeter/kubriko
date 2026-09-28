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
import kotlin.test.Test
import kotlin.test.assertEquals

class KubrikoDisposeActorsTest {

    @Test
    fun disposeReachesLiveDisposableActors() {
        val (kubriko, _) = newTestKubriko()
        val actors = List(10) { CountingActor() }
        kubriko.actorManager.add(actors)
        kubriko.actorManager.awaitProcessed()
        kubriko.dispose()
        actors.forEach {
            assertEquals(1, it.disposed.get())
            assertEquals(0, it.removed.get())
        }
    }

    @Test
    fun disposeIsCalledOnce() {
        val (kubriko, _) = newTestKubriko()
        val actor = CountingActor()
        kubriko.actorManager.add(actor)
        kubriko.actorManager.awaitProcessed()
        kubriko.actorManager.remove(actor)
        kubriko.actorManager.awaitProcessed()
        kubriko.dispose()
        assertEquals(1, actor.disposed.get())
    }

    @Test
    fun aThrowingDisposeDoesNotStopTheOthers() {
        val (kubriko, _) = newTestKubriko()
        val thrower = CountingActor(disposeAction = { throw IllegalStateException("dispose failed") })
        val others = List(3) { CountingActor() }
        kubriko.actorManager.add(listOf(thrower) + others)
        kubriko.actorManager.awaitProcessed()
        kubriko.dispose()
        others.forEach { assertEquals(1, it.disposed.get()) }
    }
}
