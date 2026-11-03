/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.foundation

/**
 * Converts colors between their stored form (`0xAARRGGBB`) and the form people type: `#RRGGBB` for an opaque color, and
 * `#AARRGGBB` for one that isn't.
 */
public object ColorHex {

    private val pattern = Regex("#?([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})")

    /**
     * [color] (`0xAARRGGBB`) as `#RRGGBB` if it is opaque, or as `#AARRGGBB` if it isn't.
     */
    public fun format(color: Long): String {
        val argb = color and 0xFFFFFFFFL
        return "#" + if (argb ushr 24 == 0xFFL) (argb and 0xFFFFFF).toString(16).uppercase().padStart(6, '0') else argb.toString(16).uppercase().padStart(8, '0')
    }

    /**
     * The color [text] names, opaque if it has six digits, or null if it isn't a color.
     */
    public fun parse(text: String): Long? = pattern.matchEntire(text.trim())?.groupValues?.get(1)?.let { digits ->
        if (digits.length == 6) 0xFF000000 or digits.toLong(16) else digits.toLong(16)
    }
}
