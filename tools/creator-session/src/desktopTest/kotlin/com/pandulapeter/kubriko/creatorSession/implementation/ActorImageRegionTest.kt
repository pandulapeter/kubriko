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

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.creatorSession.protocol.ImageRegion
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActorImageRegionTest {
    private val topLeft = SceneOffset((-100f).sceneUnit, (-50f).sceneUnit)
    private val bottomRight = SceneOffset(100f.sceneUnit, 50f.sceneUnit)
    private val image = IntSize(200, 100)

    private fun box(left: Float, top: Float, right: Float, bottom: Float) =
        AxisAlignedBoundingBox(SceneOffset(left.sceneUnit, top.sceneUnit), SceneOffset(right.sceneUnit, bottom.sceneUnit))

    private fun regionOf(vararg boxes: AxisAlignedBoundingBox, viewport: IntSize = image, alignment: Alignment = Alignment.Center) =
        ActorImageRegion.of(boxes.toList(), topLeft, bottomRight, viewport, image, alignment)

    @Test
    fun aViewportFillingTheImageMapsTheSceneOntoAllOfIt() {
        assertEquals(ImageRegion(0.25f, 0.5f, 0.5f, 0.75f), regionOf(box(-50f, 0f, 0f, 25f)))
        assertEquals(ImageRegion(0f, 0f, 1f, 1f), regionOf(box(-100f, -50f, -90f, -40f), box(90f, 40f, 100f, 50f)))
    }

    @Test
    fun aLetterboxedViewportIsPlacedByItsAlignment() {
        val viewport = IntSize(100, 100)
        assertEquals(ImageRegion(0.25f, 0f, 0.75f, 1f), regionOf(box(-100f, -50f, 100f, 50f), viewport = viewport))
        assertEquals(ImageRegion(0f, 0f, 0.5f, 1f), regionOf(box(-100f, -50f, 100f, 50f), viewport = viewport, alignment = Alignment.CenterStart))
        assertEquals(ImageRegion(0.5f, 0f, 0.625f, 0.25f), regionOf(box(-100f, -50f, -50f, -25f), viewport = viewport, alignment = Alignment.TopEnd))
    }

    @Test
    fun whatReachesPastTheViewIsClampedToIt() {
        assertEquals(ImageRegion(0.75f, 0.5f, 1f, 1f), regionOf(box(50f, 0f, 300f, 200f)))
    }

    @Test
    fun noActorsOrNoneInViewLocateNothing() {
        assertNull(regionOf())
        assertNull(regionOf(box(200f, 0f, 300f, 10f)))
        assertNull(ActorImageRegion.of(listOf(box(0f, 0f, 1f, 1f)), topLeft, topLeft, image, image))
    }
}
