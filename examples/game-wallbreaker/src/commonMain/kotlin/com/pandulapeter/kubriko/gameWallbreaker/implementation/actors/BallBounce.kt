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

import com.pandulapeter.kubriko.types.SceneUnit

/**
 * Where the ball was relative to the bounding box it collided with, and the direction it leaves in. A direction of 0 keeps
 * the ball's current direction on that axis.
 */
internal enum class BounceRegion(
    private val directionX: Int,
    private val directionY: Int,
) {
    TOP_LEFT(-1, -1),
    TOP(0, -1),
    TOP_RIGHT(-1, -1),
    LEFT(-1, 0),
    RIGHT(1, 0),
    BOTTOM_LEFT(-1, 1),
    BOTTOM(0, 1),
    BOTTOM_RIGHT(1, 1),
    ;

    fun bouncedSpeedX(currentSpeedX: Int) = if (directionX == 0) currentSpeedX else directionX

    fun bouncedSpeedY(currentSpeedY: Int) = if (directionY == 0) currentSpeedY else directionY
}

/**
 * Returns the [BounceRegion] of a ball at ([ballX], [ballY]) against the box spanning [left]..[right] and [top]..[bottom], or null
 * when the ball lies exactly on one of the box's edge lines (the direction is then left unchanged).
 */
internal fun bounceRegion(
    ballX: SceneUnit,
    ballY: SceneUnit,
    left: SceneUnit,
    top: SceneUnit,
    right: SceneUnit,
    bottom: SceneUnit,
) = when {
    ballX < left && ballY < top -> BounceRegion.TOP_LEFT
    ballX > left && ballX < right && ballY < top -> BounceRegion.TOP
    ballX > right && ballY < top -> BounceRegion.TOP_RIGHT
    ballX < left && ballY > top && ballY < bottom -> BounceRegion.LEFT
    ballX > right && ballY > top && ballY < bottom -> BounceRegion.RIGHT
    ballX < left && ballY > bottom -> BounceRegion.BOTTOM_LEFT
    ballX > left && ballX < right && ballY > bottom -> BounceRegion.BOTTOM
    ballX > right && ballY > bottom -> BounceRegion.BOTTOM_RIGHT
    else -> null
}
