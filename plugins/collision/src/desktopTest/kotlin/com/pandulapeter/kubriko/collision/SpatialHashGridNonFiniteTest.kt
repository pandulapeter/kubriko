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

import com.pandulapeter.kubriko.collision.mask.BoxCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpatialHashGridNonFiniteTest {

    @Test
    fun nanMaskDoesNotChangeTheCellSize() = assertCellSizeUnchangedBy(box(x = Float.NaN, y = 0f))

    @Test
    fun infiniteMaskDoesNotChangeTheCellSize() = assertCellSizeUnchangedBy(box(x = 0f, y = 0f, width = Float.POSITIVE_INFINITY))

    @Test
    fun candidatesMatchBruteForceWithNanMaskPresent() {
        val grid = SpatialHashGrid()
        val masks = randomBoxes()
        masks.forEach { grid.add(it) }
        val nanIndex = grid.add(box(x = Float.NaN, y = 0f))
        grid.rebuild()

        val random = Random(QUERY_SEED)
        repeat(20) {
            val minX = random.nextFloat() * AREA_SIZE
            val minY = random.nextFloat() * AREA_SIZE
            val maxX = minX + random.nextFloat() * 300f
            val maxY = minY + random.nextFloat() * 300f
            val count = grid.findCandidates(minX.sceneUnit, minY.sceneUnit, maxX.sceneUnit, maxY.sceneUnit)
            val found = (0 until count).map { grid.candidateIndices[it] }.toSet()
            val expected = masks.indices.filter { index ->
                val bounds = masks[index].axisAlignedBoundingBox
                bounds.maxXRaw >= minX && bounds.minXRaw <= maxX && bounds.maxYRaw >= minY && bounds.minYRaw <= maxY
            }.toSet()
            assertFalse(nanIndex in found)
            assertEquals(expected, found)
        }
    }

    private fun assertCellSizeUnchangedBy(nonFiniteMask: BoxCollisionMask) {
        val grid = SpatialHashGrid()
        randomBoxes().forEach { grid.add(it) }
        grid.rebuild()
        val cellSizeBefore = grid.inverseCellSize()

        grid.add(nonFiniteMask)
        grid.rebuild()

        assertTrue(grid.inverseCellSize().isFinite())
        assertEquals(cellSizeBefore, grid.inverseCellSize())
    }

    private fun SpatialHashGrid.inverseCellSize() = SpatialHashGrid::class.java.getDeclaredField("inverseCellSize")
        .apply { isAccessible = true }
        .getFloat(this)

    private fun randomBoxes(): List<BoxCollisionMask> {
        val random = Random(MASK_SEED)
        return List(1000) { box(x = random.nextFloat() * AREA_SIZE, y = random.nextFloat() * AREA_SIZE) }
    }

    private fun box(x: Float, y: Float, width: Float = 20f) = BoxCollisionMask(
        initialPosition = SceneOffset(x.sceneUnit, y.sceneUnit),
        initialSize = SceneSize(width.sceneUnit, 20f.sceneUnit),
    )

    private companion object {
        const val AREA_SIZE = 3000f
        const val MASK_SEED = 42
        const val QUERY_SEED = 7
    }
}
