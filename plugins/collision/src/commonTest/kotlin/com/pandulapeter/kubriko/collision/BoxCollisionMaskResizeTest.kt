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
import com.pandulapeter.kubriko.collision.mask.BoxCollisionMask
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.rad
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BoxCollisionMaskResizeTest {

    @Test
    fun aResizedBoxIsExactlyTheBoxBuiltAtThatSize() {
        val sizes = listOf(
            size(10f, 4f),
            size(0.1f, 300f),
            size(3f, 0f),
            size(0f, 0f),
            size(-5f, 2f),
            size(7.3f, 1e-3f),
            size(1e6f, 2.5f),
            size(10f, 4f),
        )
        val resized = BoxCollisionMask(initialPosition = offset(3f, -2f), initialSize = size(1f, 1f), initialRotation = 0.4f.rad)
        for (size in sizes) {
            resized.size = size
            val built = BoxCollisionMask(initialPosition = offset(3f, -2f), initialSize = size, initialRotation = 0.4f.rad)
            assertEquals(built.vertices, resized.vertices, "Vertices at $size")
            assertEquals(built.vertexCount, resized.vertexCount, "Vertex count at $size")
            for (i in 0 until built.vertexCount) {
                assertEquals(built.normalAt(i), resized.normalAt(i), "Normal $i at $size")
            }
            assertEquals(built.size, resized.size, "Size at $size")
            assertEquals(built.axisAlignedBoundingBox.min, resized.axisAlignedBoundingBox.min, "Bounds at $size")
            assertEquals(built.axisAlignedBoundingBox.max, resized.axisAlignedBoundingBox.max, "Bounds at $size")
            assertEquals(built.isSceneOffsetInside(offset(4f, -2f)), resized.isSceneOffsetInside(offset(4f, -2f)), "Containment at $size")
        }
    }

    @Test
    fun onlyAChangedSizeCountsAsANewShapeAndReplacesTheVerticesList() {
        val mask = BoxCollisionMask(initialSize = size(10f, 4f))
        val vertices = mask.vertices
        val revision = mask.shapeRevision

        mask.size = size(10f, 4f)
        mask.size = size(-10f, 4f)
        mask.position = offset(5f, 5f)
        mask.rotation = 1f.rad

        assertEquals(revision, mask.shapeRevision)
        assertSame(vertices, mask.vertices)

        mask.size = size(20f, 4f)

        assertEquals(revision + 1, mask.shapeRevision)
        assertNotSame(vertices, mask.vertices)
        assertEquals(offset(-5f, -2f), vertices[0])
        assertEquals(offset(-10f, -2f), mask.vertices[0])
    }

    @Test
    fun aBoxThatGrowsReachesWhatItDidntBefore() {
        val growing = BoxCollisionMask(initialSize = size(10f, 10f))
        val other = BoxCollisionMask(initialPosition = offset(20f, 0f), initialSize = size(10f, 10f))
        assertFalse(growing.hasCollisionWith(other))

        growing.size = size(40f, 10f)

        assertTrue(growing.hasCollisionWith(other))
        assertEquals(20f, growing.axisAlignedBoundingBox.max.x.raw)
    }

    @Test
    fun theSpatialGridFindsABoxByTheSizeItGrewTo() {
        val grid = SpatialHashGrid()
        val growing = BoxCollisionMask(initialSize = size(10f, 10f))
        grid.add(growing)
        repeat(20) { grid.add(BoxCollisionMask(initialPosition = offset(1_000f + it * 50f, 0f), initialSize = size(10f, 10f))) }
        grid.rebuild()
        val query = BoxCollisionMask(initialPosition = offset(200f, 0f), initialSize = size(10f, 10f))
        assertEquals(0, grid.findCandidates(query.axisAlignedBoundingBox.min.x, query.axisAlignedBoundingBox.min.y, query.axisAlignedBoundingBox.max.x, query.axisAlignedBoundingBox.max.y))

        growing.size = size(400f, 10f)
        grid.rebuild()

        assertEquals(1, grid.findCandidates(query.axisAlignedBoundingBox.min.x, query.axisAlignedBoundingBox.min.y, query.axisAlignedBoundingBox.max.x, query.axisAlignedBoundingBox.max.y))
    }

    @Test
    fun aCircleCountsANewRadiusAsANewShape() {
        val circle = CircleCollisionMask(initialRadius = 5f.sceneUnit)
        val revision = circle.shapeRevision

        circle.radius = 5f.sceneUnit
        circle.position = offset(3f, 3f)
        assertEquals(revision, circle.shapeRevision)

        circle.radius = 8f.sceneUnit
        assertEquals(revision + 1, circle.shapeRevision)
    }

    private fun size(width: Float, height: Float) = SceneSize(width.sceneUnit, height.sceneUnit)

    private fun offset(x: Float, y: Float) = SceneOffset(x.sceneUnit, y.sceneUnit)
}
