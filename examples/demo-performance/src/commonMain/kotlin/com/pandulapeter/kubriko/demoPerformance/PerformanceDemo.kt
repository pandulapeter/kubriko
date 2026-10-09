/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPerformance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoPerformance.implementation.PerformanceDemoStateHolder
import com.pandulapeter.kubriko.demoPerformance.implementation.PerformanceDemoStateHolderImpl
import com.pandulapeter.kubriko.shared.SceneEditorConnection

fun createPerformanceDemoStateHolder(
    isSceneEditorEnabled: Boolean,
    sceneEditorConnection: SceneEditorConnection?,
    isLoggingEnabled: Boolean,
): PerformanceDemoStateHolder = PerformanceDemoStateHolderImpl(
    isSceneEditorEnabled = isSceneEditorEnabled,
    sceneEditorConnection = sceneEditorConnection,
    isLoggingEnabled = isLoggingEnabled,
)

@Composable
fun PerformanceDemo(
    stateHolder: PerformanceDemoStateHolder,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as PerformanceDemoStateHolderImpl
    KubrikoViewport(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
        kubriko = stateHolder.kubriko.collectAsState().value,
        windowInsets = windowInsets,
    )
}