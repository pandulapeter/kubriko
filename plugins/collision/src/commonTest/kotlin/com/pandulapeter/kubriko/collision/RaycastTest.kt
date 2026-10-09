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

import com.pandulapeter.kubriko.collision.extensions.raycast
import com.pandulapeter.kubriko.collision.extensions.raycastDistance
import com.pandulapeter.kubriko.collision.extensions.segmentCast
import com.pandulapeter.kubriko.collision.mask.BoxCollisionMask
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.collision.mask.CollisionMask
import com.pandulapeter.kubriko.collision.mask.PointCollisionMask
import com.pandulapeter.kubriko.collision.mask.PolygonCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class RaycastTest {

    @Test
    fun rayEntersAnAxisAlignedBoxThroughItsNearEdge() {
        val box = box()

        val hit = assertNotNull(box.raycast(offset(0f, 0f), offset(5f, 0f), MAX_DISTANCE))

        assertHit(hit, box, pointX = 90f, pointY = 0f, normalX = -1f, normalY = 0f, distance = 90f)
        assertEquals(90f, listOf<CollisionMask>(box).raycastDistance(offset(0f, 0f), offset(5f, 0f), MAX_DISTANCE).raw, TOLERANCE)
    }

    @Test
    fun rayEntersARotatedBoxThroughItsSlantedEdge() {
        val box = BoxCollisionMask(
            initialPosition = offset(100f, 0f),
            initialSize = SceneSize(20f.sceneUnit, 20f.sceneUnit),
            initialRotation = (PI / 4).toFloat().rad,
        )
        val halfDiagonal = 10f * sqrt(2f)
        val expectedX = 100f - halfDiagonal + 5f

        val hit = assertNotNull(box.raycast(offset(0f, 5f), offset(1f, 0f), MAX_DISTANCE))

        assertHit(hit, box, pointX = expectedX, pointY = 5f, normalX = -INVERSE_SQRT_2, normalY = INVERSE_SQRT_2, distance = expectedX)
        assertEquals(expectedX, listOf<CollisionMask>(box).raycastDistance(offset(0f, 5f), offset(1f, 0f), MAX_DISTANCE).raw, TOLERANCE)
    }

    @Test
    fun rayEntersATriangleThroughItsSlantedEdge() {
        val triangle = triangle()
        val inverseSqrt5 = 1f / sqrt(5f)

        val hit = assertNotNull(triangle.raycast(offset(200f, 5f), offset(-1f, 0f), MAX_DISTANCE))

        assertHit(hit, triangle, pointX = 100f, pointY = 5f, normalX = inverseSqrt5, normalY = 2f * inverseSqrt5, distance = 100f)
        assertEquals(100f, listOf<CollisionMask>(triangle).raycastDistance(offset(200f, 5f), offset(-1f, 0f), MAX_DISTANCE).raw, TOLERANCE)
    }

    @Test
    fun rayEntersACircleAtTheNearIntersection() {
        val circle = circle()
        val halfChord = sqrt(100f - 9f)

        val hit = assertNotNull(circle.raycast(offset(0f, 3f), offset(1f, 0f), MAX_DISTANCE))

        assertHit(hit, circle, pointX = 100f - halfChord, pointY = 3f, normalX = -halfChord / 10f, normalY = 0.3f, distance = 100f - halfChord)
        assertEquals(100f - halfChord, listOf<CollisionMask>(circle).raycastDistance(offset(0f, 3f), offset(1f, 0f), MAX_DISTANCE).raw, TOLERANCE)
    }

    @Test
    fun rayStartingInsideAShapeHitsNothing() {
        listOf(box(), triangle(), circle()).forEach { mask ->
            assertNull(mask.raycast(offset(100f, 0f), offset(1f, 0f), MAX_DISTANCE))
            assertEquals(MAX_DISTANCE, listOf(mask).raycastDistance(offset(100f, 0f), offset(1f, 0f), MAX_DISTANCE))
        }
    }

    @Test
    fun rayParallelToAnEdgeAndOutsideTheShapeMisses() {
        val box = box()

        assertNull(box.raycast(offset(0f, -20f), offset(1f, 0f), MAX_DISTANCE))
        assertEquals(MAX_DISTANCE, listOf<CollisionMask>(box).raycastDistance(offset(0f, -20f), offset(1f, 0f), MAX_DISTANCE))
    }

    @Test
    fun shapeBeyondTheMaximumDistanceIsNotHit() {
        val shortDistance = 50f.sceneUnit
        listOf(box(), triangle(), circle()).forEach { mask ->
            assertNull(mask.raycast(offset(0f, 0f), offset(1f, 0f), shortDistance))
            assertEquals(shortDistance, listOf(mask).raycastDistance(offset(0f, 0f), offset(1f, 0f), shortDistance))
        }
    }

    @Test
    fun zeroLengthDirectionHitsNothing() {
        listOf(box(), triangle(), circle()).forEach { mask ->
            assertNull(mask.raycast(offset(0f, 0f), SceneOffset.Zero, MAX_DISTANCE))
            assertNull(listOf(mask).raycast(offset(0f, 0f), SceneOffset.Zero, MAX_DISTANCE))
            assertEquals(MAX_DISTANCE, listOf(mask).raycastDistance(offset(0f, 0f), SceneOffset.Zero, MAX_DISTANCE))
        }
    }

    @Test
    fun listOverloadsPickTheNearestMaskInEitherOrder() {
        val box = box()
        val circle = circle(x = 200f)

        listOf(listOf(box, circle), listOf(circle, box)).forEach { masks ->
            val hit = assertNotNull(masks.raycast(offset(0f, 0f), offset(1f, 0f), MAX_DISTANCE))
            assertHit(hit, box, pointX = 90f, pointY = 0f, normalX = -1f, normalY = 0f, distance = 90f)
            assertEquals(90f, masks.raycastDistance(offset(0f, 0f), offset(1f, 0f), MAX_DISTANCE).raw, TOLERANCE)
        }
    }

    @Test
    fun segmentCastEqualsARaycastAlongTheSegment() {
        val masks = listOf(box(), circle(x = 200f), triangle(x = 300f))
        val start = offset(0f, 3f)
        val end = offset(250f, 7f)
        val deltaX = end.x.raw - start.x.raw
        val deltaY = end.y.raw - start.y.raw

        val segmentHit = assertNotNull(masks.segmentCast(start, end))
        val rayHit = assertNotNull(masks.raycast(start, end - start, sqrt(deltaX * deltaX + deltaY * deltaY).sceneUnit))

        assertHit(
            hit = segmentHit,
            mask = rayHit.mask,
            pointX = rayHit.point.x.raw,
            pointY = rayHit.point.y.raw,
            normalX = rayHit.normal.x.raw,
            normalY = rayHit.normal.y.raw,
            distance = rayHit.distance.raw,
        )
    }

    @Test
    fun pointMaskIsNeverHit() {
        val point = PointCollisionMask(initialPosition = offset(100f, 0f))

        assertNull(point.raycast(offset(0f, 0f), offset(1f, 0f), MAX_DISTANCE))
        assertNull(listOf<CollisionMask>(point).raycast(offset(0f, 0f), offset(1f, 0f), MAX_DISTANCE))
        assertEquals(MAX_DISTANCE, listOf<CollisionMask>(point).raycastDistance(offset(0f, 0f), offset(1f, 0f), MAX_DISTANCE))
    }

    private fun assertHit(
        hit: RaycastHit,
        mask: CollisionMask,
        pointX: Float,
        pointY: Float,
        normalX: Float,
        normalY: Float,
        distance: Float,
    ) {
        assertSame(mask, hit.mask)
        assertEquals(pointX, hit.point.x.raw, TOLERANCE)
        assertEquals(pointY, hit.point.y.raw, TOLERANCE)
        assertEquals(normalX, hit.normal.x.raw, TOLERANCE)
        assertEquals(normalY, hit.normal.y.raw, TOLERANCE)
        assertEquals(distance, hit.distance.raw, TOLERANCE)
    }

    private fun offset(x: Float, y: Float) = SceneOffset(x.sceneUnit, y.sceneUnit)

    private fun box(x: Float = 100f) = BoxCollisionMask(
        initialPosition = offset(x, 0f),
        initialSize = SceneSize(20f.sceneUnit, 20f.sceneUnit),
    )

    /** Hull centered on its bounding box: corners at (-10, -10), (-10, 10) and (10, 0) around [x]. */
    private fun triangle(x: Float = 100f) = PolygonCollisionMask(
        vertices = listOf(offset(0f, -10f), offset(0f, 10f), offset(20f, 0f)),
        initialPosition = offset(x, 0f),
    )

    private fun circle(x: Float = 100f) = CircleCollisionMask(
        initialPosition = offset(x, 0f),
        initialRadius = 10f.sceneUnit,
    )

    private companion object {
        const val TOLERANCE = 1e-4f
        val MAX_DISTANCE = 1000f.sceneUnit
        val INVERSE_SQRT_2 = 1f / sqrt(2f)
    }
}
