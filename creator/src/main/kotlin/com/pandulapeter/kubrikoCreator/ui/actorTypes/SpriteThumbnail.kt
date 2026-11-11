/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import com.pandulapeter.kubrikoCreator.ui.components.checkerboard

/**
 * [bitmap], or its frame [frame] when [sheet] cuts it into frames, as large as fits, its pixels kept sharp and multiplied
 * by [tint] if there is one, as the game tints it, over the checkerboard that shows what is see-through.
 */
@Composable
internal fun SpriteThumbnail(
    bitmap: ImageBitmap?,
    sheet: SpriteSheet?,
    frame: Int,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) = Canvas(
    modifier = modifier.clip(RoundedCornerShape(4.dp)).checkerboard(4.dp),
) {
    if (bitmap == null) return@Canvas
    val region = frameRegionOf(sheet, frame, IntSize(bitmap.width, bitmap.height))
    val (offset, fitted) = fittedInto(region.size, size)
    drawImage(
        image = bitmap,
        srcOffset = region.topLeft,
        srcSize = region.size,
        dstOffset = IntOffset(offset.x.toInt(), offset.y.toInt()),
        dstSize = IntSize(fitted.width.toInt(), fitted.height.toInt()),
        filterQuality = FilterQuality.None,
        colorFilter = tint?.let { ColorFilter.tint(it, BlendMode.Modulate) },
    )
}
