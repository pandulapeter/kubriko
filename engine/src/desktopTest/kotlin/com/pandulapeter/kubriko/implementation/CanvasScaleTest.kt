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
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanvasScaleTest {

    @Test
    fun `an untransformed canvas covers one pixel per unit`() {
        assertEquals(1f, Canvas(ImageBitmap(4, 4)).pixelsPerLocalUnit())
    }

    @Test
    fun `a scaled and rotated canvas covers its scale`() {
        val canvas = Canvas(ImageBitmap(4, 4))
        canvas.translate(10f, 3f)
        canvas.rotate(33f)
        canvas.scale(2f, 8f)

        assertTrue(abs(canvas.pixelsPerLocalUnit() - 4f) < 1e-4f)
    }

    @Test
    fun `a degenerate transform falls back to one pixel per unit`() {
        assertEquals(1f, pixelsPerUnitOfAffine(scaleX = 0f, skewX = 0f, skewY = 0f, scaleY = 3f))
    }
}
