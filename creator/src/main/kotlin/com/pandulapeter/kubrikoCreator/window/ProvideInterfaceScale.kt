/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Draws [content] at the desktop's interface scale, by scaling the density everything under it converts its dp and sp
 * with. That is the one lever that shrinks Material as a whole - text, icons, paddings, bars and dialogs in the
 * proportions they were designed in - where scaling the typography alone would leave every row and button at the size
 * it has on a phone, around smaller text.
 */
@Composable
internal fun ProvideInterfaceScale(
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density = density.density * INTERFACE_SCALE, fontScale = density.fontScale),
        content = content,
    )
}

private const val INTERFACE_SCALE = 0.85f
