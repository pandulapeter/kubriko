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
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * How many device pixels one unit of this canvas's current coordinates covers (the square root of its transform's
 * determinant, so a non-uniform scale counts by its area), or 1 where that cannot be read.
 */
internal expect fun Canvas.pixelsPerLocalUnit(): Float

internal fun pixelsPerUnitOfAffine(scaleX: Float, skewX: Float, skewY: Float, scaleY: Float): Float {
    val determinant = abs(scaleX * scaleY - skewX * skewY)
    return if (determinant > 0f && determinant.isFinite()) sqrt(determinant) else 1f
}
