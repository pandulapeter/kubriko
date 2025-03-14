/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testCollision

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.testCollision.implementation.CollisionTestStateHolder
import com.pandulapeter.kubriko.testCollision.implementation.CollisionTestStateHolderImpl
import com.pandulapeter.kubriko.uiComponents.InfoPanel
import kubriko.examples.test_collision.generated.resources.Res
import kubriko.examples.test_collision.generated.resources.description

fun createCollisionTestStateHolder(): CollisionTestStateHolder = CollisionTestStateHolderImpl()

@Composable
fun CollisionTest(
    modifier: Modifier = Modifier,
    stateHolder: CollisionTestStateHolder = createCollisionTestStateHolder(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as CollisionTestStateHolderImpl
    KubrikoViewport(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
        kubriko = stateHolder.kubriko.collectAsState().value,
        windowInsets = windowInsets,
    )
    Column {
        Box(
            modifier = Modifier
                .windowInsetsPadding(windowInsets)
                .padding(16.dp),
        ) {
            InfoPanel(
                stringResource = Res.string.description,
                isVisible = StateHolder.isInfoPanelVisible.value,
            )
        }
    }
}