/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameWallbreaker.implementation.actors

import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BallBounceTest {

    private fun regionOf(ballX: Float, ballY: Float) = bounceRegion(
        ballX = ballX.sceneUnit,
        ballY = ballY.sceneUnit,
        left = 0f.sceneUnit,
        top = 0f.sceneUnit,
        right = 100f.sceneUnit,
        bottom = 50f.sceneUnit,
    )

    private fun assertBounce(ballX: Float, ballY: Float, expectedRegion: BounceRegion, expectedSpeedX: Int, expectedSpeedY: Int) {
        val region = regionOf(ballX, ballY)
        assertEquals(expectedRegion, region)
        for (currentSpeedX in intArrayOf(-1, 1)) for (currentSpeedY in intArrayOf(-1, 1)) {
            assertEquals(if (expectedSpeedX == 0) currentSpeedX else expectedSpeedX, region!!.bouncedSpeedX(currentSpeedX))
            assertEquals(if (expectedSpeedY == 0) currentSpeedY else expectedSpeedY, region.bouncedSpeedY(currentSpeedY))
        }
    }

    @Test
    fun topLeftCornerBouncesUpAndLeft() = assertBounce(-10f, -10f, BounceRegion.TOP_LEFT, -1, -1)

    @Test
    fun topBouncesUpAndKeepsTheHorizontalDirection() = assertBounce(50f, -10f, BounceRegion.TOP, 0, -1)

    @Test
    fun topRightCornerBouncesUpAndRight() = assertBounce(110f, -10f, BounceRegion.TOP_RIGHT, 1, -1)

    @Test
    fun leftBouncesLeftAndKeepsTheVerticalDirection() = assertBounce(-10f, 25f, BounceRegion.LEFT, -1, 0)

    @Test
    fun rightBouncesRightAndKeepsTheVerticalDirection() = assertBounce(110f, 25f, BounceRegion.RIGHT, 1, 0)

    @Test
    fun bottomLeftCornerBouncesDownAndLeft() = assertBounce(-10f, 60f, BounceRegion.BOTTOM_LEFT, -1, 1)

    @Test
    fun bottomBouncesDownAndKeepsTheHorizontalDirection() = assertBounce(50f, 60f, BounceRegion.BOTTOM, 0, 1)

    @Test
    fun bottomRightCornerBouncesDownAndRight() = assertBounce(110f, 60f, BounceRegion.BOTTOM_RIGHT, 1, 1)

    @Test
    fun positionsOnAnEdgeLineLeaveTheDirectionUnchanged() {
        assertNull(regionOf(0f, -10f))
        assertNull(regionOf(100f, -10f))
        assertNull(regionOf(-10f, 0f))
        assertNull(regionOf(-10f, 50f))
        assertNull(regionOf(0f, 60f))
        assertNull(regionOf(110f, 50f))
    }

    @Test
    fun positionsInsideTheBoxLeaveTheDirectionUnchanged() {
        assertNull(regionOf(50f, 25f))
    }
}
