/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.sprites

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import kotlin.math.floor

/**
 * Where a frame of [imageWidth] by [imageHeight] pixels lies on a canvas of [canvasSize]: in its middle, each pixel
 * [zoom] by [zoom] screen pixels, moved by [pan].
 */
internal class SpriteCanvasGeometry(
    canvasSize: Size,
    private val imageWidth: Int,
    private val imageHeight: Int,
    private val zoom: Int,
    pan: Offset,
) {
    val topLeft: Offset = Offset(
        x = floor((canvasSize.width - imageWidth * zoom) / 2 + pan.x),
        y = floor((canvasSize.height - imageHeight * zoom) / 2 + pan.y),
    )

    val imageSize: Size = Size((imageWidth * zoom).toFloat(), (imageHeight * zoom).toFloat())

    /**
     * The pixel under [position] on the canvas, inside the frame or not.
     */
    fun pixelAt(position: Offset): IntOffset = IntOffset(floor((position.x - topLeft.x) / zoom).toInt(), floor((position.y - topLeft.y) / zoom).toInt())

    /**
     * Where pixel [x], [y] starts on the canvas.
     */
    fun positionOf(x: Int, y: Int): Offset = Offset(topLeft.x + x * zoom, topLeft.y + y * zoom)

    /**
     * Where [area] (in pixels) lies on the canvas, as its top left and its size.
     */
    fun boundsOf(area: IntRect): Pair<Offset, Size> = positionOf(area.left, area.top) to Size((area.width * zoom).toFloat(), (area.height * zoom).toFloat())
}

internal fun IntRect.containsPixel(pixel: IntOffset) = pixel.x in left until right && pixel.y in top until bottom
