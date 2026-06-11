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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.IsometricGraphicsDemoStateHolder
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.IsometricGraphicsDemoStateHolderImpl
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.ui.IsometricGraphicsContent
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.uiComponents.InfoPanel
import kubriko.examples.demo_isometric_graphics.generated.resources.Res
import kubriko.examples.demo_isometric_graphics.generated.resources.description
import org.jetbrains.compose.resources.stringResource

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
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        IsometricGraphicsContent(
            stateHolder = stateHolder,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(windowInsets)
                .padding(16.dp)
                // Leave room for the top-end minimap (128.dp + its 16.dp padding + a gap) so the
                // info panel never runs underneath it on narrow screens.
                .padding(end = 144.dp),
        ) {
            InfoPanel(
                text = stringResource(Res.string.description),
                isVisible = StateHolder.isInfoPanelVisible.value,
            )
        }
    }
}
