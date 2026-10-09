/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPhysics.implementation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.demoPhysics.implementation.PlatformSpecificContent
import com.pandulapeter.kubriko.shared.SceneEditorConnection
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.uiComponents.FloatingButton
import com.pandulapeter.kubriko.uiComponents.InfoPanel
import com.pandulapeter.kubriko.uiComponents.LoadingOverlay
import kubriko.examples.demo_physics.generated.resources.Res
import kubriko.examples.demo_physics.generated.resources.description
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PhysicsDemoOverlay(
    windowInsets: WindowInsets,
    shouldShowLoadingIndicator: Boolean,
    actionType: ActionType,
    onActionTypeButtonPressed: () -> Unit,
    sceneEditorConnection: SceneEditorConnection?,
) = Box {
    LoadingOverlay(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shouldShowLoadingIndicator = shouldShowLoadingIndicator,
    )
    Column(
        modifier = Modifier
            .windowInsetsPadding(windowInsets)
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End,
    ) {
        InfoPanel(
            stringResource = Res.string.description,
            isVisible = StateHolder.isInfoPanelVisible.value,
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            if (sceneEditorConnection != null) {
                PlatformSpecificContent(
                    sceneEditorConnection = sceneEditorConnection,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            FloatingButton(
                icon = actionType.icon,
                onButtonPressed = onActionTypeButtonPressed,
                contentDescription = stringResource(actionType.contentDescription),
            )
        }
    }
}
