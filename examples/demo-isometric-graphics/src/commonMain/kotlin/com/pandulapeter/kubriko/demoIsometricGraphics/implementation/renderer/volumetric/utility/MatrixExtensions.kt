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

import androidx.compose.ui.graphics.Matrix

/** Maps a `baseW` × `baseH` image onto the parallelogram spanned from p0 towards p1 and p3. */
internal fun Matrix.setParallelogramTransform(
    p0x: Float, p0y: Float,
    p1x: Float, p1y: Float,
    p3x: Float, p3y: Float,
    baseW: Float, baseH: Float,
) {
    val v = values
    v[0] = (p1x - p0x) / baseW
    v[1] = (p1y - p0y) / baseW
    v[4] = (p3x - p0x) / baseH
    v[5] = (p3y - p0y) / baseH
    v[12] = p0x
    v[13] = p0y
    v[2] = 0f; v[3] = 0f
    v[6] = 0f; v[7] = 0f
    v[8] = 0f; v[9] = 0f; v[10] = 1f; v[11] = 0f
    v[14] = 0f; v[15] = 1f
}
