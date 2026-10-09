/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.panels.instanceManagerColumn.propertyEditors

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

private fun Char.isHexDigit() = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private fun Float.toHexChannel() = (this * 255).roundToInt().coerceIn(0, 255).toString(16).padStart(2, '0').uppercase()

internal fun Color.toHexString() = "${red.toHexChannel()}${green.toHexChannel()}${blue.toHexChannel()}"

internal fun String.parseHexColor(alpha: Float) = takeIf { it.length == 6 }?.toLongOrNull(16)?.let { value ->
    Color(
        red = ((value shr 16) and 0xFF) / 255f,
        green = ((value shr 8) and 0xFF) / 255f,
        blue = (value and 0xFF) / 255f,
        alpha = alpha,
    )
}

internal fun sanitizeHexInput(text: String) = text.filter { it.isHexDigit() }.uppercase().takeLast(6)
