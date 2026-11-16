/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.collision

import com.pandulapeter.kubriko.collision.extensions.hasCollisionWith
import com.pandulapeter.kubriko.collision.extensions.raycast
import com.pandulapeter.kubriko.collision.mask.BoxCollisionMask
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.collision.mask.EllipseCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EllipseCollisionMaskTest {

    @Test
    fun itsPolygonHasTheEllipsesAreaAndStaysCloseToItsOutline() {
        val mask = EllipseCollisionMask(initialSize = size(200f, 80f))
        var area = 0f
        for (i in 0 until mask.vertexCount) {
            val vertex = mask.vertexAt(i)
            val next = mask.vertexAt((i + 1) % mask.vertexCount)
            area += (vertex.x.raw * next.y.raw - vertex.y.raw * next.x.raw) / 2f
            val reach = (vertex.x.raw / 100f).let { it * it } + (vertex.y.raw / 40f).let { it * it }
            assertTrue(reach in 1f..1.03f, "Vertex $i is $reach of the ellipse's reach")
        }
        assertEquals(EllipseCollisionMask.VERTEX_COUNT, mask.vertexCount)
        assertEquals((PI * 100 * 40).toFloat(), area, absoluteTolerance = 1f)
        assertEquals(size(200f, 80f), mask.size)
    }

    @Test
    fun itContainsWhatIsInsideTheEllipseAlongBothAxes() {
        val mask = EllipseCollisionMask(initialPosition = offset(10f, 10f), initialSize = size(200f, 80f))
        assertTrue(mask.isSceneOffsetInside(offset(10f, 10f)))
        assertTrue(mask.isSceneOffsetInside(offset(108f, 10f)))
        assertFalse(mask.isSceneOffsetInside(offset(113f, 10f)))
        assertTrue(mask.isSceneOffsetInside(offset(10f, 49f)))
        assertFalse(mask.isSceneOffsetInside(offset(10f, 52f)))
        assertFalse(mask.isSceneOffsetInside(offset(95f, 45f)), "A corner of its bounds is outside the ellipse")
        assertEquals(-91.3f, mask.axisAlignedBoundingBox.min.x.raw, absoluteTolerance = 0.1f)
    }

    @Test
    fun itCollidesWithEveryKindOfMaskAlongItsLongAxisOnly() {
        val wide = EllipseCollisionMask(initialSize = size(200f, 40f))
        assertTrue(wide.hasCollisionWith(BoxCollisionMask(initialPosition = offset(95f, 0f), initialSize = size(10f, 10f))))
        assertTrue(wide.hasCollisionWith(CircleCollisionMask(initialPosition = offset(0f, 25f), initialRadius = 6f.sceneUnit)))
        assertFalse(wide.hasCollisionWith(CircleCollisionMask(initialPosition = offset(0f, 30f), initialRadius = 6f.sceneUnit)))
        assertTrue(wide.hasCollisionWith(EllipseCollisionMask(initialPosition = offset(0f, 30f), initialSize = size(40f, 30f))))
        assertFalse(wide.hasCollisionWith(EllipseCollisionMask(initialPosition = offset(0f, 60f), initialSize = size(40f, 30f))))
    }

    @Test
    fun aRotatedEllipseTurnsItsLongAxis() {
        val upright = EllipseCollisionMask(initialSize = size(200f, 40f), initialRotation = (PI / 2).toFloat().rad)
        assertTrue(upright.isSceneOffsetInside(offset(0f, 90f)))
        assertFalse(upright.isSceneOffsetInside(offset(90f, 0f)))
    }

    @Test
    fun aRayEntersItAtItsOutline() {
        val mask = EllipseCollisionMask(initialSize = size(200f, 80f))
        val hit = assertNotNull(mask.raycast(origin = offset(-300f, 0f), direction = offset(1f, 0f), maxDistance = 1_000f.sceneUnit))
        assertTrue(abs(hit.point.x.raw + 100f) < 2f, "The ray entered at ${hit.point}")
    }

    @Test
    fun aResizedEllipseIsTheEllipseBuiltAtThatSizeAndTheSameSizeIsNoChange() {
        val resized = EllipseCollisionMask(initialSize = size(10f, 10f))
        listOf(size(300f, 20f), size(0f, 0f), size(5f, 0f), size(-40f, 30f), size(300f, 20f)).forEach { size ->
            resized.size = size
            val built = EllipseCollisionMask(initialSize = size)
            assertEquals(built.vertices, resized.vertices, "Vertices at $size")
            assertEquals(built.axisAlignedBoundingBox.max, resized.axisAlignedBoundingBox.max, "Bounds at $size")
        }
        val vertices = resized.vertices
        val revision = resized.shapeRevision
        resized.size = size(-300f, 20f)
        assertEquals(revision, resized.shapeRevision)
        assertSame(vertices, resized.vertices)
        resized.size = size(301f, 20f)
        assertEquals(revision + 1, resized.shapeRevision)
    }

    private fun size(width: Float, height: Float) = SceneSize(width.sceneUnit, height.sceneUnit)

    private fun offset(x: Float, y: Float) = SceneOffset(x.sceneUnit, y.sceneUnit)
}
