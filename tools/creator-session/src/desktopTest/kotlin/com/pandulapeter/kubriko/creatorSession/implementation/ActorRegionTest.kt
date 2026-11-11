/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.implementation

import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActorRegionTest {
    private val topLeft = SceneOffset((-100f).sceneUnit, (-50f).sceneUnit)
    private val bottomRight = SceneOffset(100f.sceneUnit, 50f.sceneUnit)

    private fun box(left: Float, top: Float, right: Float, bottom: Float) =
        AxisAlignedBoundingBox(SceneOffset(left.sceneUnit, top.sceneUnit), SceneOffset(right.sceneUnit, bottom.sceneUnit))

    @Test
    fun theRegionCoversEveryActorAsFractionsOfTheView() {
        assertEquals(ActorRegion(0.25f, 0.5f, 0.5f, 0.75f), ActorRegion.of(listOf(box(-50f, 0f, 0f, 25f)), topLeft, bottomRight))
        assertEquals(ActorRegion(0f, 0f, 1f, 1f), ActorRegion.of(listOf(box(-100f, -50f, -90f, -40f), box(90f, 40f, 100f, 50f)), topLeft, bottomRight))
    }

    @Test
    fun whatReachesPastTheViewIsClampedToIt() {
        assertEquals(ActorRegion(0.75f, 0.5f, 1f, 1f), ActorRegion.of(listOf(box(50f, 0f, 300f, 200f)), topLeft, bottomRight))
    }

    @Test
    fun noActorsOrNoneInViewLocateNothing() {
        assertNull(ActorRegion.of(emptyList(), topLeft, bottomRight))
        assertNull(ActorRegion.of(listOf(box(200f, 0f, 300f, 10f)), topLeft, bottomRight))
        assertNull(ActorRegion.of(listOf(box(0f, 0f, 1f, 1f)), topLeft, topLeft))
    }
}
