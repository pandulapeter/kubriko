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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubrikoCreator.sprites.SpriteSheet
import kotlinx.coroutines.delay

/**
 * Where frame [index] of [sheet] lies in an image of [imageSize], or the whole image when [sheet] is null; a frame
 * outside the image is cut to it, so a sheet that no longer fits never reads past it.
 */
internal fun frameRegionOf(sheet: SpriteSheet?, index: Int, imageSize: IntSize): IntRect {
    if (sheet == null) return IntRect(IntOffset.Zero, imageSize)
    val left = (index % sheet.framesPerRow) * sheet.frameWidth
    val top = (index / sheet.framesPerRow) * sheet.frameHeight
    return IntRect(
        left = left.coerceAtMost(imageSize.width),
        top = top.coerceAtMost(imageSize.height),
        right = (left + sheet.frameWidth).coerceAtMost(imageSize.width),
        bottom = (top + sheet.frameHeight).coerceAtMost(imageSize.height),
    )
}

/**
 * Where a picture of [size] goes inside [bounds], as large as fits without changing its shape, in the middle.
 */
internal fun fittedInto(size: IntSize, bounds: Size): Pair<Offset, Size> {
    if (size.width <= 0 || size.height <= 0) return Offset.Zero to Size.Zero
    val scale = minOf(bounds.width / size.width, bounds.height / size.height)
    val fitted = Size(size.width * scale, size.height * scale)
    return Offset((bounds.width - fitted.width) / 2, (bounds.height - fitted.height) / 2) to fitted
}

/**
 * The frame of [sheet] shown now, going round at its frames per second from the first whenever [sheet] changes; always
 * the first for a sheet of one frame, and for none. Read it only where it is drawn, so nothing recomposes at the frame rate.
 */
@Composable
internal fun rememberPlayingFrame(sheet: SpriteSheet?): State<Int> {
    val frame = remember(sheet) { mutableIntStateOf(0) }
    LaunchedEffect(frame) {
        if (sheet == null || sheet.frameCount < 2 || sheet.problem != null) return@LaunchedEffect
        val frameDuration = (1000 / sheet.framesPerSecond).toLong().coerceAtLeast(1)
        while (true) {
            delay(frameDuration)
            frame.intValue = (frame.intValue + 1) % sheet.frameCount
        }
    }
    return frame
}
