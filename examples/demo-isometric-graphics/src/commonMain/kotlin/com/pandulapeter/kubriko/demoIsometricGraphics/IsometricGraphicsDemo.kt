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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.IsometricGraphicsDemoStateHolder
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.IsometricGraphicsDemoStateHolderImpl
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.PlatformSpecificContent
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.ui.Controls
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.uiComponents.FloatingButton
import com.pandulapeter.kubriko.uiComponents.InfoPanel
import com.pandulapeter.kubriko.uiComponents.LoadingOverlay
import com.pandulapeter.kubriko.uiComponents.Panel
import kubriko.examples.demo_isometric_graphics.generated.resources.Res
import kubriko.examples.demo_isometric_graphics.generated.resources.collapse_controls
import kubriko.examples.demo_isometric_graphics.generated.resources.description
import kubriko.examples.demo_isometric_graphics.generated.resources.expand_controls
import kubriko.examples.demo_isometric_graphics.generated.resources.ic_brush
import org.jetbrains.compose.resources.stringResource

fun createIsometricGraphicsDemoStateHolder(
    isSceneEditorEnabled: Boolean,
    isLoggingEnabled: Boolean,
): IsometricGraphicsDemoStateHolder = IsometricGraphicsDemoStateHolderImpl(
    isSceneEditorEnabled = isSceneEditorEnabled,
    isLoggingEnabled = isLoggingEnabled,
)

@Composable
fun IsometricGraphicsDemo(
    modifier: Modifier = Modifier,
    stateHolder: IsometricGraphicsDemoStateHolder = createIsometricGraphicsDemoStateHolder(
        isSceneEditorEnabled = true,
        isLoggingEnabled = false,
    ),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as IsometricGraphicsDemoStateHolderImpl
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        KubrikoViewport(
            modifier = Modifier.fillMaxSize(),
            kubriko = stateHolder.isometricWorldKubriko,
            windowInsets = windowInsets,
        )
        LoadingOverlay(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shouldShowLoadingIndicator = stateHolder.isometricGraphicsDemoManager.shouldShowLoadingIndicator.collectAsState().value,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InfoPanel(
                stringResource = Res.string.description,
                isVisible = StateHolder.isInfoPanelVisible.value,
            )
            Card(
                modifier = Modifier.size(128.dp),
                border = BorderStroke(
                    color = MaterialTheme.colorScheme.outline,
                    width = 1.dp,
                ),
            ) {
                KubrikoViewport(
                    modifier = Modifier
                        .scale(1.5f)
                        .rotate(-45f),
                    kubriko = stateHolder.kubriko.collectAsState().value,
                )
            }
            Spacer(
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val areControlsExpanded = stateHolder.isometricGraphicsDemoManager.areControlsExpanded.collectAsState().value
                this@Column.AnimatedVisibility(
                    visible = areControlsExpanded,
                    enter = fadeIn() + scaleIn(transformOrigin = TransformOrigin(1f, 1f)),
                    exit = scaleOut(transformOrigin = TransformOrigin(1f, 1f)) + fadeOut(),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Panel(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 16.dp, end = 16.dp),
                        ) {
                            Controls(
                                gridManager = stateHolder.gridManager,
                                isometricWorldViewportManager = stateHolder.isometricWorldViewportManager,
                                isometricGraphicsDemoManager = stateHolder.isometricGraphicsDemoManager,
                            )
                        }
                    }
                }
                FloatingButton(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    icon = Res.drawable.ic_brush,
                    isSelected = areControlsExpanded,
                    contentDescription = stringResource(if (areControlsExpanded) Res.string.collapse_controls else Res.string.expand_controls),
                    onButtonPressed = stateHolder.isometricGraphicsDemoManager::toggleControlsExpanded,
                )
            }
        }
        if (stateHolder.isometricGraphicsDemoManager.isSceneEditorEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .windowInsetsPadding(windowInsets)
                    .padding(16.dp),
            ) {
                PlatformSpecificContent()
            }
        }
    }
}