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
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PolygonCollisionMaskTest {

    @Test
    fun concaveVerticesAreReplacedByTheirConvexHull() {
        val lShape = PolygonCollisionMask(vertices = listOf(offset(0f, 0f), offset(20f, 0f), offset(20f, 10f), offset(10f, 10f), offset(10f, 20f), offset(0f, 20f)))
        val hull = PolygonCollisionMask(vertices = listOf(offset(0f, 0f), offset(20f, 0f), offset(20f, 10f), offset(10f, 20f), offset(0f, 20f)))

        assertEquals(hull.vertices.toSet(), lShape.vertices.toSet())
        assertEquals(hull.size, lShape.size)
    }

    @Test
    fun emptyPolygonHasAPointBoundingBox() {
        val position = offset(5f, 7f)
        val boundingBox = PolygonCollisionMask(initialPosition = position).axisAlignedBoundingBox

        assertEquals(position, boundingBox.min)
        assertEquals(position, boundingBox.max)
    }

    @Test
    fun zeroSizeBoxContainsOnlyItsPosition() {
        val mask = BoxCollisionMask(initialPosition = offset(5f, 7f))

        assertFalse(mask.isSceneOffsetInside(offset(1000f, 1000f)))
        assertTrue(mask.isSceneOffsetInside(offset(5f, 7f)))
    }

    @Test
    fun boxContainsOnlyPointsWithinItsSize() {
        val mask = BoxCollisionMask(initialSize = SceneSize(10f.sceneUnit, 10f.sceneUnit))

        assertTrue(mask.isSceneOffsetInside(offset(1f, 1f)))
        assertFalse(mask.isSceneOffsetInside(offset(20f, 20f)))
    }

    @Test
    fun emptyPolygonCollidesLikeAPointAtItsPosition() {
        val others = listOf<CollisionMask>(
            CircleCollisionMask(initialRadius = 10f.sceneUnit),
            BoxCollisionMask(initialSize = SceneSize(10f.sceneUnit, 10f.sceneUnit)),
        )
        listOf(offset(1f, 1f), offset(50f, 0f)).forEach { position ->
            others.forEach { other ->
                val emptyPolygon = PolygonCollisionMask(initialPosition = position)
                val point = PointCollisionMask(initialPosition = position)
                val message = "$other at $position"

                assertEquals(point.hasCollisionWith(other), emptyPolygon.hasCollisionWith(other), message)
                assertEquals(other.hasCollisionWith(point), other.hasCollisionWith(emptyPolygon), message)
                assertEquals(
                    point.collisionResultWith(other, shouldSkipAxisAlignedBoundingBoxCheck = false)?.penetration,
                    emptyPolygon.collisionResultWith(other, shouldSkipAxisAlignedBoundingBoxCheck = false)?.penetration,
                    message,
                )
            }
        }
    }

    private fun offset(x: Float, y: Float) = SceneOffset(x.sceneUnit, y.sceneUnit)
}
