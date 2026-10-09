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
import com.pandulapeter.kubriko.helpers.ManualTickSource
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.newTestKubriko
import com.pandulapeter.kubriko.testFixtures.CountingActor
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Far-away `Dynamic` actors against a 1920 × 1080 viewport centered on the origin. Without a sleep margin, an actor
 * may stray half the smaller viewport dimension (540) past its edges before it is put to sleep.
 */
class ActorManagerSleepTest {

    private lateinit var kubriko: KubrikoImpl
    private lateinit var tickSource: ManualTickSource

    private fun start(actorManager: ActorManager = ActorManager.newInstance()) {
        newTestKubriko(actorManager = actorManager).let { (kubriko, tickSource) ->
            this.kubriko = kubriko
            this.tickSource = tickSource
        }
    }

    private fun addAndTick(vararg actors: Actor, tickCount: Int = 3) {
        kubriko.actorManager.add(*actors)
        kubriko.actorManager.awaitProcessed()
        repeat(tickCount) { tickSource.tick(16) }
    }

    @AfterTest
    fun tearDown() = kubriko.dispose()

    @Test
    fun actorWithinTheDefaultMarginKeepsUpdating() {
        start()
        val actor = PositionedCountingActor(960f + 539f, 0f)

        addAndTick(actor)

        assertEquals(3, actor.updates.get())
    }

    @Test
    fun actorPastTheDefaultMarginReceivesNoUpdates() {
        start()
        val actor = PositionedCountingActor(960f + 541f, 0f)

        addAndTick(actor)

        assertEquals(0, actor.updates.get())
        assertTrue(actor !in kubriko.actorManager.activeDynamicActors.value)
    }

    @Test
    fun customMarginReplacesTheDefault() {
        start(ActorManager.newInstance(farAwayActorSleepMargin = 100f.sceneUnit))
        val withinMargin = PositionedCountingActor(960f + 99f, 0f)
        val pastMargin = PositionedCountingActor(0f, 540f + 101f)

        addAndTick(withinMargin, pastMargin)

        assertEquals(3, withinMargin.updates.get())
        assertEquals(0, pastMargin.updates.get())
    }

    @Test
    fun alwaysActiveActorUpdatesFarAway() {
        start()
        val actor = PositionedCountingActor(100_000f, 0f, isAlwaysActive = true)

        addAndTick(actor)

        assertEquals(3, actor.updates.get())
    }

    @Test
    fun actorWithoutAPositionIsNeverPutToSleep() {
        start()
        val actor = CountingActor()

        addAndTick(actor)

        assertEquals(3, actor.updates.get())
    }

    @Test
    fun disabledSleepUpdatesFarAwayActors() {
        start(ActorManager.newInstance(shouldPutFarAwayActorsToSleep = false))
        val actor = PositionedCountingActor(100_000f, 0f)

        addAndTick(actor)

        assertEquals(3, actor.updates.get())
        assertTrue(actor in kubriko.actorManager.activeDynamicActors.value)
    }

    @Test
    fun sleepingActorWakesWhenTheCameraReachesIt() {
        start()
        val actor = PositionedCountingActor(100_000f, 0f)
        addAndTick(actor)

        kubriko.viewportManager.setCameraPosition(SceneOffset(100_000f.sceneUnit, 0f.sceneUnit))
        repeat(8) { tickSource.tick(16) }

        assertTrue(actor in kubriko.actorManager.activeDynamicActors.value)
        assertTrue(actor.updates.get() > 0)
    }

    @Test
    fun actorMovingFarAwayFallsAsleepWithinTheRefreshInterval() {
        start()
        val actor = PositionedCountingActor(0f, 0f)
        addAndTick(actor)

        actor.body.position = SceneOffset(100_000f.sceneUnit, 0f.sceneUnit)
        repeat(8) { tickSource.tick(16) }
        val updates = actor.updates.get()
        repeat(3) { tickSource.tick(16) }

        assertEquals(updates, actor.updates.get())
    }
}
