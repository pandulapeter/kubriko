/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPerformance.implementation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.demoPerformance.implementation.PlatformSpecificContent
import com.pandulapeter.kubriko.shared.SceneEditorConnection
import com.pandulapeter.kubriko.shared.ui.LocalInfoPanelVisibility
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.uiComponents.InfoPanel
import com.pandulapeter.kubriko.uiComponents.LoadingOverlay
import com.pandulapeter.kubriko.uiComponents.Panel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kubriko.examples.demo_performance.generated.resources.Res
import kubriko.examples.demo_performance.generated.resources.description

@Composable
internal fun PerformanceDemoOverlay(
    windowInsets: WindowInsets,
    shouldShowLoadingIndicator: Boolean,
    areActorsLoaded: Boolean,
    totalRuntimeInMilliseconds: StateFlow<Long>,
    getViewportTopLeft: () -> SceneOffset,
    getViewportBottomRight: () -> SceneOffset,
    getAllVisibleActors: () -> List<Visible>,
    getAllVisibleActorsWithinViewport: () -> List<Visible>,
    getAllActiveDynamicActors: () -> List<Visible>,
    sceneEditorConnection: SceneEditorConnection?,
) = Box {
    LoadingOverlay(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shouldShowLoadingIndicator = shouldShowLoadingIndicator,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(windowInsets)
            .padding(16.dp),
    ) {
        InfoPanel(
            stringResource = Res.string.description,
            isVisible = LocalInfoPanelVisibility.current,
        )
        AnimatedVisibility(
            visible = areActorsLoaded,
            enter = fadeIn() + scaleIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            Panel {
                MiniMap(
                    miniMapSize = 120.dp,
                    dotRadius = 1.5.dp,
                    gameTime = remember(totalRuntimeInMilliseconds) { totalRuntimeInMilliseconds.filter { it % 2 == 0L } }.collectAsState(0L).value,
                    visibleActorColor = MaterialTheme.colorScheme.primary,
                    invisibleActorColor = lerp(LocalContentColor.current, MaterialTheme.colorScheme.surface, 0.6f),
                    inactiveActorColor = lerp(LocalContentColor.current, MaterialTheme.colorScheme.surface, 0.9f),
                    getViewportTopLeft = getViewportTopLeft,
                    getViewportBottomRight = getViewportBottomRight,
                    getAllVisibleActors = getAllVisibleActors,
                    getAllVisibleActorsWithinViewport = getAllVisibleActorsWithinViewport,
                    getAllActiveDynamicActors = getAllActiveDynamicActors,
                )
            }
        }
    }
    if (sceneEditorConnection != null) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(windowInsets)
                .padding(16.dp),
        ) {
            PlatformSpecificContent(
                sceneEditorConnection = sceneEditorConnection,
            )
        }
    }
}
