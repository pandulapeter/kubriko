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

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas

// Vertex colors override the paint when no shader is set, so a plain paint is all that's needed.
private val trianglePaint = android.graphics.Paint()

// Hardware-accelerated since API 29; minSdk is 29. The framework's vertexCount is the number of
// floats consumed from [positions], so oversized arrays draw without trimming — zero copies.
internal actual fun drawTriangles(canvas: Canvas, positions: FloatArray, colors: IntArray, vertexCount: Int) =
    canvas.nativeCanvas.drawVertices(
        android.graphics.Canvas.VertexMode.TRIANGLES,
        vertexCount * 2,
        positions,
        0,
        null,
        0,
        colors,
        0,
        null,
        0,
        0,
        trianglePaint,
    )
