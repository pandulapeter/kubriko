/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.uiComponents.theme

import androidx.compose.ui.graphics.Color

/**
 * The brand colors of the Kubriko visual style, used by [KubrikoTheme] to build its color schemes.
 */
object KubrikoColors {
    /** The main brand color: `primary` and `primaryContainer` in both color schemes, and the background of the loading screen. */
    val brandPrimary = Color(0xFF6060AA)

    /** The color of content drawn on top of [brandPrimary]: `onPrimary` and `onPrimaryContainer` in both color schemes. */
    val onBrandPrimary = Color(0xFFFDFDFD)

    /** A lighter variant of [brandPrimary], used as `secondary` in the dark color scheme only. */
    val brandSecondary = Color(0xFF9090CC)
}