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
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.helpers.TickSource
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.testFixtures.CountingActor
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import kotlin.test.Test
import kotlin.test.assertEquals

class DisposeDuringTickTest {

    private class CountingManager : Manager() {
        var updates = 0

        override fun onUpdate(deltaTimeInMilliseconds: Int) {
            updates++
        }
    }

    private class DisposingManager : Manager() {
        var isArmed = false
        private lateinit var kubriko: Kubriko

        override fun onInitialize(kubriko: Kubriko) {
            this.kubriko = kubriko
        }

        override fun onUpdate(deltaTimeInMilliseconds: Int) {
            if (isArmed) kubriko.dispose()
        }
    }

    private class DisposingActor : Actor, Dynamic {
        private lateinit var kubriko: Kubriko

        override fun onAdded(kubriko: Kubriko) {
            this.kubriko = kubriko
        }

        override fun update(deltaTimeInMilliseconds: Int) = kubriko.dispose()
    }

    @Test
    fun disposingFromUpdateEndsTheTick() {
        val countingManager = CountingManager()
        val (kubriko, tickSource) = newTestKubriko(countingManager)
        kubriko.actorManager.add(DisposingActor(), CountingActor())
        kubriko.actorManager.awaitProcessed()
        val updatesBefore = countingManager.updates
        tickSource.tick(16)
        assertEquals(updatesBefore, countingManager.updates)
        tickSource.tick(16)
        assertEquals(updatesBefore, countingManager.updates)
    }

    @Test
    fun disposingFromAManagerEndsTheTick() {
        val disposingManager = DisposingManager()
        val countingManager = CountingManager()
        val tickSource = TickSource.manual()
        val kubriko = Kubriko.newInstance(
            disposingManager,
            ActorManager.newInstance(shouldPutFarAwayActorsToSleep = false),
            countingManager,
            tickSource = tickSource,
        ) as KubrikoImpl
        tickSource.start()
        val actor = CountingActor()
        kubriko.actorManager.add(actor)
        kubriko.actorManager.awaitProcessed()
        disposingManager.isArmed = true
        tickSource.tick(16)
        assertEquals(0, countingManager.updates)
        assertEquals(0, actor.updates.get())
        tickSource.tick(16)
        assertEquals(0, countingManager.updates)
    }
}
