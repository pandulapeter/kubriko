/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoContentShaders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoContentShaders.implementation.ContentShadersDemoStateHolder
import com.pandulapeter.kubriko.demoContentShaders.implementation.ContentShadersDemoStateHolderImpl
import com.pandulapeter.kubriko.shared.ui.ShadersNotSupportedMessage

fun createContentShadersDemoStateHolder(
    isLoggingEnabled: Boolean,
): ContentShadersDemoStateHolder = ContentShadersDemoStateHolderImpl(
    isLoggingEnabled = isLoggingEnabled,
)

@Composable
fun ContentShadersDemo(
    stateHolder: ContentShadersDemoStateHolder,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as ContentShadersDemoStateHolderImpl
    if (stateHolder.shaderManager.areShadersSupported) {
        KubrikoViewport(
            modifier = modifier.background(Color.Black),
            windowInsets = windowInsets,
            kubriko = stateHolder.kubriko.collectAsState().value,
        )
    } else {
        ShadersNotSupportedMessage(
            modifier = modifier,
            windowInsets = windowInsets,
        )
    }
}