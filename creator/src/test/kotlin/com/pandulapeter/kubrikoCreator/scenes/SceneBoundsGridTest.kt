/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SceneBoundsGridTest {
    private val settings = SceneHitTest.Bounds(0, -500f, -500f, 500f, 500f, drawingOrder = Float.MAX_VALUE, isBackground = true)
    private val ball = SceneHitTest.Bounds(1, -10f, -10f, 10f, 10f)
    private val wall = SceneHitTest.Bounds(2, 0f, -50f, 100f, 50f)
    private val overlay = SceneHitTest.Bounds(3, 5f, 5f, 15f, 15f, drawingOrder = -1f)

    private fun gridOf(vararg bounds: SceneHitTest.Bounds) = SceneBoundsGrid(cellSize = 16f).apply { rebuild(bounds.toList()) }

    @Test
    fun theActorDrawnOnTopIsPicked() {
        val grid = gridOf(settings, ball, wall, overlay)
        assertEquals(1, grid.pick(-5f, 0f))
        // Of equal drawing orders, the later one is drawn above.
        assertEquals(2, grid.pick(5f, 0f))
        assertEquals(3, grid.pick(8f, 8f))
        assertEquals(2, grid.pick(50f, 0f))
    }

    @Test
    fun theBackgroundIsOnlyPickedWhereNothingElseIs() {
        assertEquals(0, gridOf(settings, ball).pick(200f, 200f))
        assertEquals(1, gridOf(ball, settings).pick(0f, 0f))
        assertNull(gridOf(settings, ball).pick(600f, 0f))
        assertNull(gridOf().pick(0f, 0f))
    }

    @Test
    fun boundsOnACellsEdgeArePickedFromBothCells() {
        val grid = gridOf(SceneHitTest.Bounds(0, 10f, 10f, 16f, 16f))
        assertEquals(0, grid.pick(15.9f, 15.9f))
        assertEquals(0, grid.pick(16f, 16f))
        assertNull(grid.pick(16.1f, 16f))
    }

    @Test
    fun negativeCoordinatesHaveCellsOfTheirOwn() {
        val grid = gridOf(SceneHitTest.Bounds(0, -40f, -40f, -20f, -20f), SceneHitTest.Bounds(1, 20f, 20f, 40f, 40f))
        assertEquals(0, grid.pick(-30f, -30f))
        assertEquals(1, grid.pick(30f, 30f))
        assertNull(grid.pick(-30f, 30f))
    }

    @Test
    fun anActorCoveringManyCellsIsStillPicked() {
        val floor = SceneHitTest.Bounds(0, -10_000f, 0f, 10_000f, 20f)
        val grid = gridOf(floor, ball)
        assertEquals(0, grid.pick(9_000f, 10f))
        assertEquals(1, grid.pick(0f, 5f))
        grid.update(0, null)
        assertNull(grid.pick(9_000f, 10f))
    }

    @Test
    fun updatedBoundsArePickedWhereTheyAreNow() {
        val grid = gridOf(ball, wall)
        grid.update(1, ball.copy(left = 200f, top = 200f, right = 220f, bottom = 220f))
        assertNull(grid.pick(-5f, 0f))
        assertEquals(1, grid.pick(210f, 210f))
        grid.update(2, null)
        assertNull(grid.pick(50f, 0f))
        assertEquals(listOf(1), grid.all().map { it.index })
    }

    @Test
    fun rebuildingForgetsWhatWasThere() {
        val grid = gridOf(ball, wall)
        grid.rebuild(listOf(overlay.copy(index = 0)))
        assertEquals(0, grid.pick(10f, 10f))
        assertNull(grid.pick(50f, 0f))
    }
}
