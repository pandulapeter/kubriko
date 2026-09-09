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

private var shaderSource: ImageBitmap? = null
private var shaderPaint: Paint? = null

// Rebuilt only when the pattern itself changes, which for a catalog generated once per process is never.
// Mipmapped sampling is what lets a pattern average itself away as the camera pulls back instead of aliasing
// into a shimmer.
private fun texturePaintFor(texture: ImageBitmap, replace: Boolean): Paint {
    if (shaderSource !== texture) {
        val image = Image.makeFromBitmap(texture.asSkiaBitmap())
        shaderPaint = Paint().apply {
            color = -1
            shader = image.makeShader(
                FilterTileMode.REPEAT,
                FilterTileMode.REPEAT,
                FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR),
            )
        }
        shaderSource = texture
    }
    return shaderPaint!!.apply { blendMode = if (replace) BlendMode.SRC else BlendMode.SRC_OVER }
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
