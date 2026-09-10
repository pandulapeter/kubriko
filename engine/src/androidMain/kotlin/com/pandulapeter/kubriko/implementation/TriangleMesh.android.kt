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

import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.nativeCanvas

// Vertex colors override the paint when no shader is set, so a plain paint is all that's needed.
// Opaque white ensures that on devices where the vertex colors are modulated with the paint
// (rather than replacing it), they pass through unchanged.
private val trianglePaint = Paint().apply { color = android.graphics.Color.WHITE }

// The source-replace variant; overlapping shapes occlude rather than blend inside a group's offscreen buffer.
private val replaceTrianglePaint = Paint().apply {
    color = android.graphics.Color.WHITE
    blendMode = android.graphics.BlendMode.SRC
}

// Mipmapped sampling is what lets a pattern average itself away as the camera pulls back instead of aliasing
// into a shimmer, and the anisotropy is what asks for it: a BitmapShader built any other way samples at
// SkMipmapMode.kNone whatever the bitmap says about mipmaps, so bilinear is all it ever gets. It is also the
// right filter for a plane seen at a grazing angle, which a square filter kernel has to blur or alias through.
private const val MAX_ANISOTROPY = 4

// A handful of identity-keyed slots rather than one: two textured batches drawn in a stable A, B order
// evicted each other from a single slot, rebuilding the Paint and BitmapShader on every draw. The fixed
// size keeps native resources bounded, and an evicted entry is dropped exactly as the single slot was.
private const val TEXTURE_PAINT_CACHE_SIZE = 4
private val texturePaintSources = arrayOfNulls<ImageBitmap>(TEXTURE_PAINT_CACHE_SIZE)
private val texturePaints = arrayOfNulls<Paint>(TEXTURE_PAINT_CACHE_SIZE)
private var nextTexturePaintSlot = 0

private fun texturePaintFor(texture: ImageBitmap, replace: Boolean): Paint {
    for (i in texturePaintSources.indices) {
        if (texturePaintSources[i] === texture) {
            return texturePaints[i]!!.withBlendMode(replace)
        }
    }
    val bitmap = texture.asAndroidBitmap().apply { setHasMipMap(true) }
    val paint = Paint().apply {
        color = android.graphics.Color.WHITE
        // The fallback sampling wherever the anisotropic path is not honoured; a shader built with
        // FILTER_MODE_DEFAULT takes its filter from the paint.
        isFilterBitmap = true
        shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                maxAnisotropy = MAX_ANISOTROPY
            } else {
                filterMode = BitmapShader.FILTER_MODE_LINEAR
            }
        }
    }
    texturePaintSources[nextTexturePaintSlot] = texture
    texturePaints[nextTexturePaintSlot] = paint
    nextTexturePaintSlot = (nextTexturePaintSlot + 1) % TEXTURE_PAINT_CACHE_SIZE
    return paint.withBlendMode(replace)
}

private fun Paint.withBlendMode(replace: Boolean) = apply {
    blendMode = if (replace) android.graphics.BlendMode.SRC else android.graphics.BlendMode.SRC_OVER
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
    // The framework modulates the vertex colors with the shader wherever both are given, which is exactly
    // how the pattern is meant to apply: it tints the geometry's own color rather than replacing it.
    canvas.nativeCanvas.drawVertices(
        android.graphics.Canvas.VertexMode.TRIANGLES,
        vertexCount * 2,
        positions,
        0,
        if (texture == null) null else texCoords,
        0,
        colors,
        0,
        indices,
        0,
        indexCount,
        paint,
    )
}
