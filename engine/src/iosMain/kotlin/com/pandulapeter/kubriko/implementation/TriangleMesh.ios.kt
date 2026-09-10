/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.implementation

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.skiaCanvas
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Paint
import org.jetbrains.skia.VertexMode

// MODULATE multiplies the vertex colors with the opaque white paint, i.e. uses them as-is.
private val trianglePaint = Paint().apply { color = -1 }

// The source-replace variant; the vertex colors still pass through unchanged.
private val replaceTrianglePaint = Paint().apply { color = -1; blendMode = BlendMode.SRC }

// A handful of identity-keyed slots rather than one: two textured batches drawn in a stable A, B order
// evicted each other from a single slot, rebuilding the image wrapper and shader on every draw. The fixed
// size keeps native resources bounded, and an evicted entry is dropped exactly as the single slot was.
private const val TEXTURE_PAINT_CACHE_SIZE = 4
private val texturePaintSources = arrayOfNulls<ImageBitmap>(TEXTURE_PAINT_CACHE_SIZE)
private val texturePaints = arrayOfNulls<Paint>(TEXTURE_PAINT_CACHE_SIZE)
private var nextTexturePaintSlot = 0

// Mipmapped sampling is what lets a pattern average itself away as the camera pulls back instead of aliasing
// into a shimmer.
private fun texturePaintFor(texture: ImageBitmap, replace: Boolean): Paint {
    for (i in texturePaintSources.indices) {
        if (texturePaintSources[i] === texture) {
            return texturePaints[i]!!.apply { blendMode = if (replace) BlendMode.SRC else BlendMode.SRC_OVER }
        }
    }
    val image = Image.makeFromBitmap(texture.asSkiaBitmap())
    val paint = Paint().apply {
        color = -1
        shader = image.makeShader(
            FilterTileMode.REPEAT,
            FilterTileMode.REPEAT,
            FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR),
        )
    }
    texturePaintSources[nextTexturePaintSlot] = texture
    texturePaints[nextTexturePaintSlot] = paint
    nextTexturePaintSlot = (nextTexturePaintSlot + 1) % TEXTURE_PAINT_CACHE_SIZE
    return paint.apply { blendMode = if (replace) BlendMode.SRC else BlendMode.SRC_OVER }
}

internal actual fun drawTriangles(
    canvas: Canvas,
    positions: FloatArray,
    colors: IntArray,
    indices: ShortArray,
    vertexCount: Int,
    indexCount: Int,
    texCoords: FloatArray?,
    texture: ImageBitmap?,
    replace: Boolean,
    isAntiAlias: Boolean,
) {
    val paint = if (texture != null) {
        texturePaintFor(texture, replace)
    } else {
        if (replace) replaceTrianglePaint else trianglePaint
    }
    paint.isAntiAlias = isAntiAlias
    TriangleMeshBuffers.prepare(positions, colors, indices, vertexCount, indexCount, texCoords)
    canvas.skiaCanvas.drawVertices(
        VertexMode.TRIANGLES,
        TriangleMeshBuffers.positions,
        TriangleMeshBuffers.colors,
        TriangleMeshBuffers.texCoords,
        TriangleMeshBuffers.indices,
        BlendMode.MODULATE,
        paint,
    )
}
