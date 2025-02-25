/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoContentShaders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoContentShaders.implementation.ContentShadersDemoStateHolder
import com.pandulapeter.kubriko.demoContentShaders.implementation.ContentShadersDemoStateHolderImpl
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageVector
import kubriko.examples.demo_content_shaders.generated.resources.Res
import kubriko.examples.demo_content_shaders.generated.resources.ic_brush
import kubriko.examples.demo_content_shaders.generated.resources.shaders_not_supported
import org.jetbrains.compose.resources.stringResource

fun createContentShadersDemoStateHolder(): ContentShadersDemoStateHolder = ContentShadersDemoStateHolderImpl()

@Composable
fun ContentShadersDemo(
    modifier: Modifier = Modifier,
    stateHolder: ContentShadersDemoStateHolder = createContentShadersDemoStateHolder(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as ContentShadersDemoStateHolderImpl
    if (stateHolder.shaderManager.areShadersSupported) {
        preloadedImageVector(Res.drawable.ic_brush) // TODO: Introduce a loading state
        KubrikoViewport(
            modifier = modifier.background(Color.Black),
            windowInsets = windowInsets,
            kubriko = stateHolder.kubriko.collectAsState().value,
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .padding(16.dp),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .align(Alignment.Center),
                textAlign = TextAlign.Center,
                text = stringResource(Res.string.shaders_not_supported),
            )
        }
    }
}