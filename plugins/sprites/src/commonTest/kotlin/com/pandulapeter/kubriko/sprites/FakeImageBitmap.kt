/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sprites

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces

/**
 * An [ImageBitmap] with no pixels behind it, standing in for a decoded sprite on test classpaths without a Skia runtime.
 */
internal class FakeImageBitmap : ImageBitmap {
    override val width = 1
    override val height = 1
    override val colorSpace: ColorSpace = ColorSpaces.Srgb
    override val hasAlpha = true
    override val config = ImageBitmapConfig.Argb8888

    override fun readPixels(buffer: IntArray, startX: Int, startY: Int, width: Int, height: Int, bufferOffset: Int, stride: Int) = Unit

    override fun prepareToDraw() = Unit
}
