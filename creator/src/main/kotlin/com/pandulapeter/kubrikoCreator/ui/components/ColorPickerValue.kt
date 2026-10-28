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

import com.pandulapeter.kubrikoCreator.actors.ColorHex

/**
 * What the color picker shows: the color, and the hex code being typed, which may not be a color yet.
 *
 * @param isAlphaEnabled Whether the color may be translucent; if not, it is always opaque.
 */
internal data class ColorPickerValue(
    val color: HsvColor,
    val text: String,
    val isAlphaEnabled: Boolean = true,
) {
    val argb get() = color.toArgb()

    val isTextValid get() = ColorHex.parse(text) != null

    /**
     * Picked with the sliders: the hex code follows.
     */
    fun withColor(color: HsvColor) = (if (isAlphaEnabled) color else color.copy(alpha = 1f)).let { copy(color = it, text = ColorHex.format(it.toArgb())) }

    /**
     * Typed as a hex code: the color follows once [text] is one, keeping the hue for a gray.
     */
    fun withText(text: String) = ColorHex.parse(text)?.let { argb ->
        val parsed = HsvColor.fromArgb(argb, hue = color.hue)
        copy(color = if (isAlphaEnabled) parsed else parsed.copy(alpha = 1f), text = text)
    } ?: copy(text = text)

    companion object {

        fun of(argb: Long, isAlphaEnabled: Boolean = true) = ColorPickerValue(HsvColor.fromArgb(argb), text = "", isAlphaEnabled).let { it.withColor(it.color) }
    }
}
