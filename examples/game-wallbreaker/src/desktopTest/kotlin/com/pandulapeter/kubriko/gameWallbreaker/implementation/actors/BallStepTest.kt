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

import kotlin.test.Test
import kotlin.test.assertTrue

class BallStepTest {

    private val maximumStep = Ball.MaximumSpeed.raw * Ball.MAXIMUM_MOVEMENT_DELTA_IN_MILLISECONDS

    @Test
    fun maximumStepFitsInsideThePaddleWindow() {
        assertTrue(maximumStep < (Paddle.Height + Ball.Radius * 2).raw)
    }

    @Test
    fun maximumStepFitsInsideTheBrickWindow() {
        assertTrue(maximumStep < (Brick.Height + Ball.Radius * 2).raw)
    }
}
