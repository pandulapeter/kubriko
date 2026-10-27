/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

/**
 * Converts opaque colors between their stored form (`0xAARRGGBB`) and the `#RRGGBB` form people type.
 */
internal object ColorHex {

    private val pattern = Regex("#?([0-9A-Fa-f]{6})")

    fun format(color: Long) = "#" + (color and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')

    fun parse(text: String) = pattern.matchEntire(text.trim())?.let { 0xFF000000 or it.groupValues[1].toLong(16) }
}
