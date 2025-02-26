/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.debugMenu

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoTheme

/**
 * TODO: Documentation
 */
@Composable
fun KubrikoViewportWithDebugMenu(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    kubriko: Kubriko?,
    isEnabled: Boolean = true,
    buttonAlignment: Alignment? = Alignment.TopStart,
    debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit = { KubrikoTheme(content = it) },
    kubrikoViewport: @Composable () -> Unit,
) = BoxWithConstraints(
    modifier = modifier,
) {
    if (kubriko != null && isEnabled) {
        val isColumn = maxWidth < maxHeight
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                modifier = Modifier.weight(1f),
            ) {
                KubrikoViewportWithDebugMenuOverlay(
                    modifier = Modifier.weight(1f),
                    kubriko = kubriko,
                    kubrikoViewport = kubrikoViewport,
                    buttonAlignment = buttonAlignment,
                )
                VerticalDebugMenu(
                    kubriko = kubriko,
                    isEnabled = !isColumn,
                    windowInsets = windowInsets,
                    debugMenuTheme = debugMenuTheme,
                )
            }
            HorizontalDebugMenu(
                kubriko = kubriko,
                isEnabled = isColumn,
                windowInsets = windowInsets,
                debugMenuTheme = debugMenuTheme,
            )
        }
    } else {
        kubrikoViewport()
    }
}