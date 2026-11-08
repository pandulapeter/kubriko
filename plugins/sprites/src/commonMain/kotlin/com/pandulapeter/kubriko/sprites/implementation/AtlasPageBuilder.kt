/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sprites.implementation

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.helpers.extensions.markImmutable

/** Copies sprites onto one atlas page, at the places an [AtlasLayout] gave them, and returns the finished page. */
internal fun interface AtlasPageBuilder {
    fun build(width: Int, height: Int, sprites: List<ImageBitmap>, xs: IntArray, ys: IntArray, padding: Int): ImageBitmap
}

/**
 * Draws the sprites onto a new bitmap through a Compose [Canvas], each with its padding (see [forEachAtlasBlit]), and
 * marks it immutable, since a bitmap that was drawn into would otherwise be copied again on every draw on the Skia
 * targets.
 */
internal object CanvasAtlasPageBuilder : AtlasPageBuilder {

    override fun build(width: Int, height: Int, sprites: List<ImageBitmap>, xs: IntArray, ys: IntArray, padding: Int): ImageBitmap {
        val page = ImageBitmap(width, height)
        val canvas = Canvas(page)
        // A new Skia bitmap's pixels are not cleared, and the gaps between cells would otherwise hold whatever was there.
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply { blendMode = BlendMode.Clear })
        val paint = Paint().apply {
            blendMode = BlendMode.Src
            filterQuality = FilterQuality.None
        }
        for (index in sprites.indices) {
            val sprite = sprites[index]
            forEachAtlasBlit(sprite.width, sprite.height, xs[index], ys[index], padding) { srcX, srcY, srcWidth, srcHeight, dstX, dstY, dstWidth, dstHeight ->
                canvas.drawImageRect(
                    image = sprite,
                    srcOffset = IntOffset(srcX, srcY),
                    srcSize = IntSize(srcWidth, srcHeight),
                    dstOffset = IntOffset(dstX, dstY),
                    dstSize = IntSize(dstWidth, dstHeight),
                    paint = paint,
                )
            }
        }
        return page.markImmutable()
    }
}
