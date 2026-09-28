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

import com.pandulapeter.kubriko.collision.extensions.collisionResultWith
import com.pandulapeter.kubriko.collision.extensions.hasCollisionWith
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
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PointMaskCollisionTest {

    private val circle = CircleCollisionMask(initialRadius = 10f.sceneUnit)
    private val box = BoxCollisionMask(initialSize = SceneSize(10f.sceneUnit, 10f.sceneUnit))

    @Test
    fun pointCollidesWithCircle() {
        assertCollidesInBothOrders(point(1f, 1f), circle)
        assertDoesNotCollideInBothOrders(point(20f, 0f), circle)
    }

    @Test
    fun pointCollidesWithBox() {
        assertCollidesInBothOrders(point(1f, 1f), box)
        assertDoesNotCollideInBothOrders(point(20f, 20f), box)
    }

    @Test
    fun pointCollidesWithRotatedBoxOnlyWhereItsRotatedShapeIs() {
        val rotatedBox = BoxCollisionMask(
            initialSize = SceneSize(10f.sceneUnit, 10f.sceneUnit),
            initialRotation = (PI.toFloat() / 4f).rad,
        )
        val point = point(6f, 0f)

        assertFalse(box.hasCollisionWith(point))
        assertCollidesInBothOrders(point, rotatedBox)
    }

    @Test
    fun pointToCircleResultMatchesCircleToCircle() {
        val point = point(8f, 0f)
        val result = assertNotNull(point.collisionResultWith(circle, shouldSkipAxisAlignedBoundingBoxCheck = false))
        val circleToCircleResult = assertNotNull(
            CircleCollisionMask(initialPosition = point.position, initialRadius = 0.001f.sceneUnit)
                .collisionResultWith(circle, shouldSkipAxisAlignedBoundingBoxCheck = false)
        )
        val swappedResult = assertNotNull(circle.collisionResultWith(point, shouldSkipAxisAlignedBoundingBoxCheck = false))

        assertEquals(2f, result.penetration.raw, TOLERANCE)
        assertEquals(point.position, result.contact)
        assertEquals(circleToCircleResult.contactNormal, result.contactNormal)
        assertEquals(1f, abs(result.contactNormal.x.raw), TOLERANCE)
        assertEquals(0f, result.contactNormal.y.raw, TOLERANCE)
        assertEquals(-result.contactNormal, swappedResult.contactNormal)
    }

    @Test
    fun pointsDoNotCollideWithEachOther() {
        assertDoesNotCollideInBothOrders(point(1f, 1f), point(1f, 1f))
    }

    @Test
    fun emptyPolygonCollidesLikeAPoint() {
        assertCollidesInBothOrders(PolygonCollisionMask(initialPosition = SceneOffset(1f.sceneUnit, 1f.sceneUnit)), circle)
    }

    private fun assertCollidesInBothOrders(a: CollisionMask, b: CollisionMask) {
        assertTrue(a.hasCollisionWith(b))
        assertTrue(b.hasCollisionWith(a))
    }

    private fun assertDoesNotCollideInBothOrders(a: CollisionMask, b: CollisionMask) {
        assertFalse(a.hasCollisionWith(b))
        assertFalse(b.hasCollisionWith(a))
    }

    private fun point(x: Float, y: Float) = PointCollisionMask(SceneOffset(x.sceneUnit, y.sceneUnit))

    private companion object {
        const val TOLERANCE = 0.001f
    }
}
