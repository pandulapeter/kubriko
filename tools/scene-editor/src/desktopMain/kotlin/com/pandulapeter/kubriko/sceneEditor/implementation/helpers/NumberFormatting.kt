/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.helpers

import java.util.Locale
import kotlin.math.roundToInt

internal fun formatEditorNumber(value: Float, shouldRound: Boolean): String =
    if (shouldRound) value.roundToInt().toString() else String.format(Locale.ROOT, "%.2f", value)

internal fun parseEditorNumber(text: String): Float? =
    text.trim().replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() }
