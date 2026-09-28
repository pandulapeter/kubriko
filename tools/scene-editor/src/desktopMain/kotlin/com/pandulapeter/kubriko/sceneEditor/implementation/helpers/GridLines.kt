/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.helpers

import kotlin.math.floor

internal const val MIN_GRID_LINE_SPACING_PX = 4f
internal const val MAX_GRID_LINES_PER_AXIS = 2000L

internal fun firstGridLineIndex(min: Float, cellSize: Float): Long = floor(min.toDouble() / cellSize).toLong()

internal fun lastGridLineIndex(max: Float, cellSize: Float): Long = floor(max.toDouble() / cellSize).toLong()

internal fun isGridLineVisible(cellSize: Float, scale: Float) = cellSize * scale >= MIN_GRID_LINE_SPACING_PX

/**
 * Returns 1 when every grid line is far enough apart to be seen, 10 when only the major lines are, and 0 when
 * nothing on this axis should be drawn.
 */
internal fun gridLineStep(cellSize: Float, scale: Float): Long = when {
    isGridLineVisible(cellSize, scale) -> 1L
    isGridLineVisible(cellSize * 10, scale) -> 10L
    else -> 0L
}

/**
 * Returns the first line index to draw: [firstIndex] itself, or the next major line when only majors are drawn.
 */
internal fun alignGridLineIndex(firstIndex: Long, step: Long): Long =
    if (step <= 1L) firstIndex else firstIndex + Math.floorMod(-firstIndex, step)
