/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.sprites.pixels

import kotlin.math.roundToInt

/**
 * The frames around the one being drawn, faint and tinted (the ones before it reddish, the ones after it bluish), as one
 * image the sprite editor draws under it, so an animation's motion shows while each frame is drawn.
 */
public object OnionSkin {
    public const val PREVIOUS_TINT: Int = 0xFFE03C3C.toInt()
    public const val NEXT_TINT: Int = 0xFF3C64E0.toInt()

    /**
     * The frames of [frames] up to [reach] before and after frame [index] (not going round), each a mix of its colors and
     * its tint, the nearest at [opacity] and each farther one fainter, nearer ones over farther ones; null when there is no
     * such frame, or nothing to show of them.
     */
    public fun of(frames: List<PixelImage>, index: Int, reach: Int, opacity: Float): PixelImage? {
        val shown = frames.getOrNull(index) ?: return null
        if (reach < 1 || opacity <= 0f) return null
        val pixels = IntArray(shown.width * shown.height)
        var isEmpty = true
        for (distance in reach downTo 1) {
            val strength = opacity.coerceAtMost(1f) * (reach - distance + 1) / reach
            frames.getOrNull(index - distance)?.let { isEmpty = !paint(pixels, it, PREVIOUS_TINT, strength) && isEmpty }
            frames.getOrNull(index + distance)?.let { isEmpty = !paint(pixels, it, NEXT_TINT, strength) && isEmpty }
        }
        return if (isEmpty) null else PixelImage(shown.width, shown.height, pixels)
    }

    /**
     * Puts [frame], tinted and at [strength], over [pixels], returning whether it had any pixel to put.
     */
    private fun paint(pixels: IntArray, frame: PixelImage, tint: Int, strength: Float): Boolean {
        var isPainted = false
        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                val color = frame[x, y]
                val alpha = (color ushr 24) / 255f * strength
                if (alpha <= 0f) continue
                isPainted = true
                val index = y * frame.width + x
                pixels[index] = over(pixels[index], mixed(color, tint), alpha)
            }
        }
        return isPainted
    }

    private fun mixed(color: Int, tint: Int): Int = (channel(color, 16) + channel(tint, 16)) / 2 shl 16 or
        ((channel(color, 8) + channel(tint, 8)) / 2 shl 8) or
        ((channel(color, 0) + channel(tint, 0)) / 2)

    /**
     * The color [rgb] at [alpha] put over [below], as a painter puts a see-through color over another.
     */
    private fun over(below: Int, rgb: Int, alpha: Float): Int {
        val belowAlpha = (below ushr 24) / 255f
        val resultAlpha = alpha + belowAlpha * (1 - alpha)
        if (resultAlpha <= 0f) return 0
        fun blend(shift: Int) = ((channel(rgb, shift) * alpha + channel(below, shift) * belowAlpha * (1 - alpha)) / resultAlpha).roundToInt().coerceIn(0, 255)
        return ((resultAlpha * 255).roundToInt() shl 24) or (blend(16) shl 16) or (blend(8) shl 8) or blend(0)
    }

    private fun channel(color: Int, shift: Int) = color ushr shift and 0xFF
}
