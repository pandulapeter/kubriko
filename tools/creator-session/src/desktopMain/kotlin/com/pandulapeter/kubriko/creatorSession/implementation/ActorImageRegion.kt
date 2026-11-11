/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.implementation

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.pandulapeter.kubriko.actor.body.AxisAlignedBoundingBox
import com.pandulapeter.kubriko.creatorSession.protocol.ImageRegion
import com.pandulapeter.kubriko.types.SceneOffset

/**
 * Where Actors are in a captured image of the game's window, from what the game's viewport shows.
 */
internal object ActorImageRegion {

    /**
     * The region of an image [imageSize] pixels large that [bounds] take up together, clamped to the viewport, or null when
     * there are none or none of them is in view. The viewport shows the scene from [viewTopLeft] to [viewBottomRight] in
     * [viewportSize] pixels, placed in the image by [alignment] (centered unless a fixed aspect ratio aligns it otherwise),
     * since a letterboxed viewport doesn't fill the window the image is taken of.
     */
    fun of(
        bounds: List<AxisAlignedBoundingBox>,
        viewTopLeft: SceneOffset,
        viewBottomRight: SceneOffset,
        viewportSize: IntSize,
        imageSize: IntSize,
        alignment: Alignment = Alignment.Center,
    ): ImageRegion? {
        if (bounds.isEmpty() || imageSize.width <= 0 || imageSize.height <= 0) return null
        val sceneWidth = viewBottomRight.x.raw - viewTopLeft.x.raw
        val sceneHeight = viewBottomRight.y.raw - viewTopLeft.y.raw
        if (sceneWidth <= 0f || sceneHeight <= 0f) return null
        val viewport = if (viewportSize.width in 1..imageSize.width && viewportSize.height in 1..imageSize.height) viewportSize else imageSize
        val offset = alignment.align(viewport, imageSize, LayoutDirection.Ltr)
        fun x(scene: Float) = (offset.x + ((scene - viewTopLeft.x.raw) / sceneWidth).coerceIn(0f, 1f) * viewport.width) / imageSize.width
        fun y(scene: Float) = (offset.y + ((scene - viewTopLeft.y.raw) / sceneHeight).coerceIn(0f, 1f) * viewport.height) / imageSize.height
        val region = ImageRegion(
            left = x(bounds.minOf { it.minXRaw }),
            top = y(bounds.minOf { it.minYRaw }),
            right = x(bounds.maxOf { it.maxXRaw }),
            bottom = y(bounds.maxOf { it.maxYRaw }),
        )
        return region.takeIf { it.right > it.left && it.bottom > it.top }
    }
}
