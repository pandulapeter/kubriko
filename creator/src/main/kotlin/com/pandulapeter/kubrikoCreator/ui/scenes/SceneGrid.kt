/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubrikoCreator.settings.CreatorSettings
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The Scene editor's grid, as the Creator settings set it.
 *
 * @param size The side of a square, in scene units.
 * @param isSnappingByDefault Whether a Scene opens with snapping to it on.
 */
internal data class SceneGrid(
    val size: Float,
    val isSnappingByDefault: Boolean,
) {
    /**
     * [value] rounded to the nearest line of the grid.
     */
    fun snap(value: Float): Float = (value / size).roundToInt() * size

    /**
     * [point] moved to the nearest crossing of the grid.
     */
    fun snap(point: Offset): Offset = Offset(snap(point.x), snap(point.y))

    /**
     * How far apart the lines drawn are, in scene units: the grid's own, or for a view zoomed out so far that they would
     * be closer than [minimumSpacingInPixels] on the screen ([pixelsPerUnit] to a scene unit), every second, fourth, and
     * so on, so the grid never fills the Scene.
     */
    fun drawnSpacing(pixelsPerUnit: Float, minimumSpacingInPixels: Float): Float {
        var spacing = size
        while (spacing * pixelsPerUnit < minimumSpacingInPixels) spacing *= 2
        return spacing
    }
}

/**
 * The grid of the Creator settings, provided once above the workspace.
 */
internal val LocalSceneGrid = compositionLocalOf { CreatorSettings().let { SceneGrid(it.sceneGridSize.toFloat(), it.isSnappingToGrid) } }

/**
 * Draws [grid]'s lines over what the canvas shows, with [toScene] and [toScreen] converting between pixels and Scene
 * units.
 */
internal fun DrawScope.drawSceneGrid(grid: SceneGrid, color: Color, minimumSpacingInPixels: Float, toScene: (Offset) -> Offset, toScreen: (Offset) -> Offset) {
    val topLeft = toScene(Offset.Zero)
    val bottomRight = toScene(Offset(size.width, size.height))
    val visibleWidth = bottomRight.x - topLeft.x
    if (visibleWidth <= 0f) return
    val pixelsPerUnit = size.width / visibleWidth
    val spacing = grid.drawnSpacing(pixelsPerUnit, minimumSpacingInPixels)
    for (index in gridLineIndices(topLeft.x, bottomRight.x, spacing)) {
        val screenX = toScreen(Offset(index * spacing, 0f)).x
        drawLine(color, Offset(screenX, 0f), Offset(screenX, size.height))
    }
    for (index in gridLineIndices(topLeft.y, bottomRight.y, spacing)) {
        val screenY = toScreen(Offset(0f, index * spacing)).y
        drawLine(color, Offset(0f, screenY), Offset(size.width, screenY))
    }
}

/**
 * Which lines of a grid [spacing] apart lie between [start] and [end], by their index from the origin: counted rather
 * than added up, since far from the origin adding the spacing to a float can leave it as it was.
 */
internal fun gridLineIndices(start: Float, end: Float, spacing: Float): LongRange = ceil(start.toDouble() / spacing).toLong()..floor(end.toDouble() / spacing).toLong()
