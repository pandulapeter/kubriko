/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.actor.looks

import androidx.compose.ui.graphics.ImageBitmap
import com.pandulapeter.kubriko.helpers.extensions.meanArgb

/**
 * The mean colors of the textures sprites have recently been too small to show, so each is read back once rather than
 * once per sprite. Keyed by identity in a fixed number of slots, which bounds what it keeps alive. Used from the
 * drawing thread only.
 */
internal object TextureMeanColors {
    private const val SLOT_COUNT = 32

    /** A texture whose pixels cannot be read back (an Android hardware bitmap) stands in as mid-gray. */
    private const val UNREADABLE_ARGB = 0xFF808080.toInt()

    private val textures = arrayOfNulls<ImageBitmap>(SLOT_COUNT)
    private val meanArgbs = IntArray(SLOT_COUNT)
    private var nextSlot = 0

    fun argbOf(texture: ImageBitmap): Int {
        for (i in textures.indices) {
            if (textures[i] === texture) return meanArgbs[i]
        }
        val argb = try {
            texture.meanArgb()
        } catch (_: Exception) {
            UNREADABLE_ARGB
        }
        textures[nextSlot] = texture
        meanArgbs[nextSlot] = argb
        nextSlot = (nextSlot + 1) % SLOT_COUNT
        return argb
    }
}

/** Multiplies two ARGB colors channel by channel, alpha included, as a modulating tint does. */
internal fun modulateArgb(first: Int, second: Int): Int {
    val alpha = ((first ushr 24) * (second ushr 24) + 127) / 255
    val red = (((first shr 16) and 0xFF) * ((second shr 16) and 0xFF) + 127) / 255
    val green = (((first shr 8) and 0xFF) * ((second shr 8) and 0xFF) + 127) / 255
    val blue = ((first and 0xFF) * (second and 0xFF) + 127) / 255
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}
