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

/**
 * An image drawn as text, a row per line: `.` is see-through, and every other letter a color of its own ([colorOf]).
 */
internal fun image(vararg rows: String) = PixelImage(rows.first().length, rows.size, rows.joinToString("").map(::colorOf).toIntArray())

/**
 * [this] image as text, as [image] draws one.
 */
internal fun PixelImage.rows() = List(height) { y -> String(CharArray(width) { x -> letterOf(this[x, y]) }) }

private val letters = "abcdefghijklmnopqrstuvwxyz"

internal fun colorOf(letter: Char) = if (letter == '.') PixelImage.TRANSPARENT else (0xFF000000.toInt() or (letters.indexOf(letter) + 1))

private fun letterOf(color: Int) = if (color == PixelImage.TRANSPARENT) '.' else letters[(color and 0xFF) - 1]
