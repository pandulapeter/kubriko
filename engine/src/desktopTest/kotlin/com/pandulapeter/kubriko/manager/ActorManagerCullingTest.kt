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
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [ActorManager.visibleActorsWithinViewport] against a 1920 × 1080 viewport centered on the origin: x in [-960, 960],
 * y in [-540, 540].
 */
class ActorManagerCullingTest {

    private lateinit var kubriko: KubrikoImpl
    private lateinit var tickSource: ManualTickSource

    private fun start(
        actorManager: ActorManager = ActorManager.newInstance(),
        viewportManager: ViewportManager = ViewportManager.newInstance(),
    ) {
        newTestKubriko(viewportManager, actorManager = actorManager).let { (kubriko, tickSource) ->
            this.kubriko = kubriko
            this.tickSource = tickSource
        }
    }

    private fun addAndTick(vararg actors: Actor) {
        kubriko.actorManager.add(*actors)
        kubriko.actorManager.awaitProcessed()
        tickSource.tick(16)
    }

    private fun visibleActors() = kubriko.actorManager.visibleActorsWithinViewport.value.toSet()

    @AfterTest
    fun tearDown() = kubriko.dispose()

    @Test
    fun publishesExactlyTheActorsWhoseBoundsTouchTheViewport() {
        start()
        val inside = TestVisible(0f, 0f)
        val touchingRight = TestVisible(960f, 0f)
        val touchingLeft = TestVisible(-970f, 0f)
        val touchingTop = TestVisible(0f, -550f)
        val touchingBottom = TestVisible(0f, 540f)
        val pastRight = TestVisible(960.5f, 0f)
        val pastTop = TestVisible(0f, -550.5f)

        addAndTick(inside, touchingRight, touchingLeft, touchingTop, touchingBottom, pastRight, pastTop)

        assertEquals(setOf(inside, touchingRight, touchingLeft, touchingTop, touchingBottom), visibleActors())
    }

    @Test
    fun alwaysVisibleActorIsPublishedWherever() {
        start()
        val farAway = TestVisible(100_000f, 100_000f, isAlwaysVisible = true)

        addAndTick(farAway)

        assertEquals(setOf(farAway), visibleActors())
    }

    @Test
    fun edgeBufferWidensTheVisibleArea() {
        start(viewportManager = ViewportManager.newInstance(viewportEdgeBuffer = 50f.sceneUnit))
        val withinBuffer = TestVisible(1010f, 0f)
        val pastBuffer = TestVisible(1010.5f, 0f)

        addAndTick(withinBuffer, pastBuffer)

        assertEquals(setOf(withinBuffer), visibleActors())
    }

    @Test
    fun removedActorLeavesTheVisibleActorsOnTheNextTick() {
        start()
        val actor = TestVisible(0f, 0f)
        addAndTick(actor)

        kubriko.actorManager.remove(actor)
        kubriko.actorManager.awaitProcessed()
        tickSource.tick(16)

        assertTrue(visibleActors().isEmpty())
    }

    @Test
    fun actorMovingIntoViewIsPublishedWithinTheRefreshInterval() {
        start()
        val actor = TestVisible(5_000f, 0f)
        addAndTick(actor)
        assertFalse(actor in visibleActors())

        actor.moveTo(0f, 0f)
        repeat(8) { tickSource.tick(16) }

        assertTrue(actor in visibleActors())
    }

    @Test
    fun zeroRefreshIntervalRecullsOnEveryTick() {
        start(actorManager = ActorManager.newInstance(invisibleActorMinimumRefreshTimeInMillis = 0))
        val actor = TestVisible(0f, 0f)
        addAndTick(actor)

        actor.moveTo(5_000f, 0f)
        tickSource.tick(16)

        assertFalse(actor in visibleActors())
    }

    @Test
    fun cameraMovementIsReflectedWithinTheRefreshInterval() {
        start()
        val actor = TestVisible(5_000f, 0f)
        addAndTick(actor)

        kubriko.viewportManager.setCameraPosition(SceneOffset(5_000f.sceneUnit, 0f.sceneUnit))
        repeat(8) { tickSource.tick(16) }

        assertEquals(setOf(actor), visibleActors())
    }

    @Test
    fun headlessInstanceStillCullsVisibleActors() {
        start(actorManager = ActorManager.newInstance(shouldComposeLayers = false))
        val inside = TestVisible(0f, 0f)
        val outside = TestVisible(5_000f, 0f)

        addAndTick(inside, outside)

        assertEquals(setOf(inside), visibleActors())
    }
}
