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
import com.pandulapeter.kubriko.collision.mask.CollisionMask
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SpatialHashGridTest {

    @Test
    fun candidatesMatchBruteForceForSmallQueries() = assertMatchesBruteForce(randomBoxes(), maximumQuerySize = 300f)

    @Test
    fun candidatesMatchBruteForceForQueriesCoveringTheWholeWorld() = assertMatchesBruteForce(randomBoxes(), maximumQuerySize = 100_000f)

    @Test
    fun candidatesMatchBruteForceWithAMaskSpanningTheWholeMap() = assertMatchesBruteForce(
        masks = randomBoxes() + box(x = AREA_SIZE / 2, y = 0f, width = AREA_SIZE * 2),
        maximumQuerySize = 300f,
    )

    @Test
    fun candidatesMatchBruteForceWithANanMask() = assertMatchesBruteForce(
        masks = randomBoxes() + box(x = Float.NaN, y = 0f),
        maximumQuerySize = 300f,
    )

    @Test
    fun candidatesMatchBruteForceWithAnInfinitelyWideMask() = assertMatchesBruteForce(
        masks = randomBoxes() + box(x = 0f, y = 100f, width = Float.POSITIVE_INFINITY),
        maximumQuerySize = 300f,
    )

    @Test
    fun movedMaskIsFoundAtItsNewPositionAfterARebuild() {
        val grid = SpatialHashGrid()
        val mask = box(x = 0f, y = 0f)
        val index = grid.add(mask)
        randomBoxes().forEach { grid.add(it) }
        grid.rebuild()

        mask.position = SceneOffset(5_000f.sceneUnit, 5_000f.sceneUnit)
        grid.rebuild()

        assertEquals(listOf(index), grid.query(4_990f, 4_990f, 5_010f, 5_010f))
        assertSame(mask, grid.maskAt(index))
    }

    @Test
    fun clearRemovesEveryMask() {
        val grid = SpatialHashGrid()
        randomBoxes().forEach { grid.add(it) }
        grid.rebuild()

        grid.clear()
        grid.rebuild()

        assertEquals(0, grid.maskCount)
        assertEquals(emptyList(), grid.query(-100_000f, -100_000f, 100_000f, 100_000f))
    }

    private fun assertMatchesBruteForce(masks: List<CollisionMask>, maximumQuerySize: Float) {
        val grid = SpatialHashGrid()
        masks.forEach { grid.add(it) }
        grid.rebuild()
        val random = Random(QUERY_SEED)
        repeat(50) { queryIndex ->
            val minX = random.nextFloat() * AREA_SIZE * 2 - AREA_SIZE
            val minY = random.nextFloat() * AREA_SIZE * 2 - AREA_SIZE
            val maxX = minX + random.nextFloat() * maximumQuerySize
            val maxY = minY + random.nextFloat() * maximumQuerySize

            val found = grid.query(minX, minY, maxX, maxY)

            val expected = masks.indices.filter { index ->
                val bounds = masks[index].axisAlignedBoundingBox
                bounds.maxXRaw >= minX && bounds.minXRaw <= maxX && bounds.maxYRaw >= minY && bounds.minYRaw <= maxY
            }
            assertEquals(expected, found.sorted(), "Query $queryIndex")
        }
    }

    private fun SpatialHashGrid.query(minX: Float, minY: Float, maxX: Float, maxY: Float): List<Int> {
        val count = findCandidates(minX.sceneUnit, minY.sceneUnit, maxX.sceneUnit, maxY.sceneUnit)
        return List(count) { candidateIndices[it] }
    }

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
