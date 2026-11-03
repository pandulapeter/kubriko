/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * A color as the color picker edits it. Hue is kept apart from the color, so that it doesn't jump back to red while a
 * gray (or black) is being picked.
 *
 * @param hue From 0 to 360.
 * @param saturation From 0 to 1.
 * @param brightness From 0 to 1.
 * @param alpha From 0 to 1.
 */
internal data class HsvColor(
    val hue: Float,
    val saturation: Float,
    val brightness: Float,
    val alpha: Float = 1f,
) {
    /**
     * As `0xAARRGGBB`.
     */
    fun toArgb(): Long {
        val chroma = brightness * saturation
        val sector = (hue.coerceIn(0f, 360f) / 60f) % 6f
        val second = chroma * (1f - abs(sector % 2f - 1f))
        val (red, green, blue) = when (sector.toInt()) {
            0 -> Triple(chroma, second, 0f)
            1 -> Triple(second, chroma, 0f)
            2 -> Triple(0f, chroma, second)
            3 -> Triple(0f, second, chroma)
            4 -> Triple(second, 0f, chroma)
            else -> Triple(chroma, 0f, second)
        }
        val offset = brightness - chroma
        return (channel(alpha) shl 24) or (channel(red + offset) shl 16) or (channel(green + offset) shl 8) or channel(blue + offset)
    }

    companion object {

        /**
         * [argb] (`0xAARRGGBB`) as an [HsvColor], with [hue] where the color has none (a gray).
         */
        fun fromArgb(argb: Long, hue: Float = 0f): HsvColor {
            val alpha = ((argb ushr 24) and 0xFF) / 255f
            val red = ((argb ushr 16) and 0xFF) / 255f
            val green = ((argb ushr 8) and 0xFF) / 255f
            val blue = (argb and 0xFF) / 255f
            val max = maxOf(red, green, blue)
            val chroma = max - minOf(red, green, blue)
            val colorHue = when {
                chroma == 0f -> hue
                max == red -> 60f * (((green - blue) / chroma + 6f) % 6f)
                max == green -> 60f * ((blue - red) / chroma + 2f)
                else -> 60f * ((red - green) / chroma + 4f)
            }
            return HsvColor(colorHue, if (max == 0f) 0f else chroma / max, max, alpha)
        }

        private fun channel(value: Float) = (value.coerceIn(0f, 1f) * 255f).roundToLong()
    }
}
