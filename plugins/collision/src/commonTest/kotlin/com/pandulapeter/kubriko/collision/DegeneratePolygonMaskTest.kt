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
import com.pandulapeter.kubriko.collision.mask.PolygonCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DegeneratePolygonMaskTest {

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
    fun regularBoxStillContainsItsInside() {
        val mask = BoxCollisionMask(initialSize = SceneSize(10f.sceneUnit, 10f.sceneUnit))

        assertTrue(mask.isSceneOffsetInside(offset(1f, 1f)))
        assertFalse(mask.isSceneOffsetInside(offset(20f, 20f)))
    }

    @Test
    fun emptyPolygonDoesNotCrashTheNarrowPhase() {
        val emptyPolygon = PolygonCollisionMask()
        val others = listOf<CollisionMask>(
            CircleCollisionMask(initialRadius = 10f.sceneUnit),
            BoxCollisionMask(initialSize = SceneSize(10f.sceneUnit, 10f.sceneUnit)),
        )
        others.forEach { other ->
            emptyPolygon.hasCollisionWith(other)
            other.hasCollisionWith(emptyPolygon)
            emptyPolygon.collisionResultWith(other, shouldSkipAxisAlignedBoundingBoxCheck = false)
            other.collisionResultWith(emptyPolygon, shouldSkipAxisAlignedBoundingBoxCheck = false)
        }
    }

    private fun offset(x: Float, y: Float) = SceneOffset(x.sceneUnit, y.sceneUnit)
}
