/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.volumetric.utility

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

// Manually built mip levels for a face texture. Compose's drawImage samples the full-resolution
// bitmap regardless of how small it appears on screen, so heavily minified faces (zoomed-out
// camera) pay full memory bandwidth and shimmer. Levels are built lazily, each by halving the
// previous one, and selected from the current world zoom.
internal class TextureMipChain(base: ImageBitmap) {

    private val levels = arrayOfNulls<ImageBitmap>(LEVEL_COUNT).also { it[0] = base }

    // World zoom approximates the world-unit-to-pixel multiplier; each halving of the zoom halves
    // the on-screen texel density, so the next mip level becomes indistinguishable but much
    // cheaper to sample.
    fun forZoom(zoom: Float): ImageBitmap = level(
        when {
            zoom <= 0.25f -> 2
            zoom <= 0.5f -> 1
            else -> 0
        }
    )

    private fun level(index: Int): ImageBitmap {
        levels[index]?.let { return it }
        val source = level(index - 1)
        if (source.width <= 1 && source.height <= 1) return source
        val width = (source.width / 2).coerceAtLeast(1)
        val height = (source.height / 2).coerceAtLeast(1)
        val target = ImageBitmap(width, height)
        Canvas(target).drawImageRect(
            image = source,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(source.width, source.height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(width, height),
            paint = downscalePaint,
        )
        levels[index] = target
        return target
    }

    private companion object {
        const val LEVEL_COUNT = 3
        val downscalePaint = Paint().apply { filterQuality = FilterQuality.Low }
    }
}
