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
import com.pandulapeter.kubriko.helpers.TriangleBatch

/**
 * Rasterizes the first [vertexCount] unique vertices and [indexCount] indices in a single native draw call;
 * every three consecutive indices form one triangle.
 *
 * The position/color arrays may be larger than needed — implementations that cannot pass a count (Skia through
 * Skiko) trim through [TriangleMeshBuffers], while Android draws straight from the given arrays so the
 * per-frame hot path never copies or allocates. [isAntiAlias] toggles the paint's anti-aliasing (edge smoothing
 * beyond what [TriangleBatch.addLine]'s geometric fringes provide). With [replace] the triangles use
 * source-replace compositing instead of the default source-over. [texCoords] and [texture] are either both
 * present — the vertex colors are then modulated by the pattern sampled at each vertex's own coordinate, in
 * texels — or both absent, which is the plain color-only path.
 */
internal expect fun drawTriangles(
    canvas: Canvas,
    positions: FloatArray,
    colors: IntArray,
    indices: ShortArray,
    vertexCount: Int,
    indexCount: Int,
    texCoords: FloatArray? = null,
    texture: ImageBitmap? = null,
    replace: Boolean = false,
    isAntiAlias: Boolean = false,
)
