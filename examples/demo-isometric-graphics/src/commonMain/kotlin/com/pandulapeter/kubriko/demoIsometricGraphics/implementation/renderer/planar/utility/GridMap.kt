/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.planar.utility

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap

// Precomputed, camera-independent companion data for the static ground map texture: the PixelMap
// and per-cell grid-edge flags, computed once instead of re-deriving them from neighbor pixel
// reads on every frame. Shared by drawTopDownGrid (minimap) and drawIsometricGrid (main view).
class GridMap(val image: ImageBitmap) {
    val width = image.width
    val height = image.height
    val pixelMap = image.toPixelMap()
    val edgeFlags = ByteArray(width * height)

    init {
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                val color = pixelMap[i, j]
                if (color.alpha > 0f) {
                    var flags = 0
                    if (j == 0 || pixelMap[i, j - 1] != color) flags = flags or EDGE_TOP
                    if (i == width - 1 || pixelMap[i + 1, j] != color) flags = flags or EDGE_RIGHT
                    if (j == height - 1 || pixelMap[i, j + 1] != color) flags = flags or EDGE_BOTTOM
                    if (i == 0 || pixelMap[i - 1, j] != color) flags = flags or EDGE_LEFT
                    edgeFlags[index] = flags.toByte()
                }
                index++
            }
        }
    }

    companion object {
        const val EDGE_TOP = 1
        const val EDGE_RIGHT = 2
        const val EDGE_BOTTOM = 4
        const val EDGE_LEFT = 8
    }
}
