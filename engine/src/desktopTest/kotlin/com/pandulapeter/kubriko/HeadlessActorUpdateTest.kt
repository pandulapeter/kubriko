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

import androidx.compose.ui.geometry.Size
import com.pandulapeter.kubriko.actor.body.PointBody
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.helpers.TickSource
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.testFixtures.CountingActor
import com.pandulapeter.kubriko.testFixtures.awaitProcessed
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeadlessActorUpdateTest {

    private class FarActor : CountingActor(), Positionable {
        override val body = PointBody(initialPosition = SceneOffset(100_000f.sceneUnit, 100_000f.sceneUnit))
    }

    @Test
    fun dynamicActorsUpdateWithoutAViewport() {
        val tickSource = TickSource.manual()
        val kubriko = Kubriko.newInstance(tickSource = tickSource) as KubrikoImpl
        tickSource.start()
        val a = CountingActor()
        kubriko.actorManager.add(a)
        kubriko.actorManager.awaitProcessed()
        tickSource.tick(16)
        assertEquals(1, a.updates.get())
        assertTrue(kubriko.actorManager.visibleActorsWithinViewport.value.isEmpty())
        kubriko.dispose()
    }

    @Test
    fun documentedManualExampleUpdatesOnTheFirstTick() = repeat(200) {
        val a = CountingActor()
        val tickSource = TickSource.manual()
        val kubriko = Kubriko.newInstance(
            ActorManager.newInstance(initialActors = listOf(a)),
            tickSource = tickSource,
        )
        tickSource.start()
        tickSource.tick(16)
        assertEquals(1, a.updates.get())
        kubriko.dispose()
    }

    @Test
    fun sleepStartsOnceAViewportIsMeasured() {
        val tickSource = TickSource.manual()
        val kubriko = Kubriko.newInstance(tickSource = tickSource) as KubrikoImpl
        tickSource.start()
        val far = FarActor()
        kubriko.actorManager.add(far)
        kubriko.actorManager.awaitProcessed()
        tickSource.tick(16)
        assertEquals(1, far.updates.get())
        kubriko.viewportManager.updateSize(Size(1920f, 1080f))
        tickSource.tick(16)
        tickSource.tick(16)
        assertFalse(far in kubriko.actorManager.activeDynamicActors.value)
        val updates = far.updates.get()
        tickSource.tick(16)
        assertEquals(updates, far.updates.get())
        kubriko.dispose()
    }
}
