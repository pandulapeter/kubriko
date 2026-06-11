/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.IsometricGraphicsDemoStateHolder
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.IsometricGraphicsDemoStateHolderImpl
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.ui.IsometricGraphicsContent

fun createIsometricGraphicsDemoStateHolder(
    isLoggingEnabled: Boolean,
): IsometricGraphicsDemoStateHolder = IsometricGraphicsDemoStateHolderImpl(
    isLoggingEnabled = isLoggingEnabled,
)

@Composable
fun IsometricGraphicsDemo(
    modifier: Modifier = Modifier,
    stateHolder: IsometricGraphicsDemoStateHolder = createIsometricGraphicsDemoStateHolder(
        isLoggingEnabled = false,
    ),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as IsometricGraphicsDemoStateHolderImpl
    IsometricGraphicsContent(
        stateHolder = stateHolder,
        modifier = modifier.fillMaxSize(),
        windowInsets = windowInsets,
    )
}
