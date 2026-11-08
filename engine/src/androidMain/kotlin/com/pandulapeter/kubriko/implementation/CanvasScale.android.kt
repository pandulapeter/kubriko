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

import android.graphics.Matrix
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas

private val matrix = Matrix()
private val matrixValues = FloatArray(9)

@Suppress("DEPRECATION")
internal actual fun Canvas.pixelsPerLocalUnit(): Float {
    nativeCanvas.getMatrix(matrix)
    matrix.getValues(matrixValues)
    return pixelsPerUnitOfAffine(
        scaleX = matrixValues[Matrix.MSCALE_X],
        skewX = matrixValues[Matrix.MSKEW_X],
        skewY = matrixValues[Matrix.MSKEW_Y],
        scaleY = matrixValues[Matrix.MSCALE_Y],
    )
}
