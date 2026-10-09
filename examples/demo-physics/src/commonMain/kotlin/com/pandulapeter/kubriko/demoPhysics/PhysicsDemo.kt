/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPhysics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.demoPhysics.implementation.PhysicsDemoStateHolder
import com.pandulapeter.kubriko.demoPhysics.implementation.PhysicsDemoStateHolderImpl
import com.pandulapeter.kubriko.shared.SceneEditorConnection

fun createPhysicsDemoStateHolder(
    isSceneEditorEnabled: Boolean,
    sceneEditorConnection: SceneEditorConnection?,
    isLoggingEnabled: Boolean,
): PhysicsDemoStateHolder = PhysicsDemoStateHolderImpl(
    isSceneEditorEnabled = isSceneEditorEnabled,
    sceneEditorConnection = sceneEditorConnection,
    isLoggingEnabled = isLoggingEnabled,
)

@Composable
fun PhysicsDemo(
    stateHolder: PhysicsDemoStateHolder,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as PhysicsDemoStateHolderImpl
    KubrikoViewport(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
        kubriko = stateHolder.kubriko.collectAsState().value,
        windowInsets = windowInsets,
    )
}