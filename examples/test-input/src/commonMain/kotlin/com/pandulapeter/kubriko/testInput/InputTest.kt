/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testInput

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.testInput.implementation.InputTestStateHolder
import com.pandulapeter.kubriko.testInput.implementation.InputTestStateHolderImpl
import com.pandulapeter.kubriko.testInput.implementation.ui.Keyboard
import com.pandulapeter.kubriko.uiComponents.InfoPanel
import kubriko.examples.test_input.generated.resources.Res
import kubriko.examples.test_input.generated.resources.description

fun createInputTestStateHolder(
    isLoggingEnabled: Boolean,
): InputTestStateHolder = InputTestStateHolderImpl(
    isLoggingEnabled = isLoggingEnabled,
)

@Composable
fun InputTest(
    modifier: Modifier = Modifier,
    stateHolder: InputTestStateHolder = createInputTestStateHolder(
        isLoggingEnabled = false,
    ),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    stateHolder as InputTestStateHolderImpl
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
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
        Keyboard(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Start + WindowInsetsSides.End + WindowInsetsSides.Bottom))
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            activeKeys = stateHolder.inputTestManager.activeKeys.collectAsState().value,
        )
    }
    KubrikoViewport(
        kubriko = stateHolder.kubriko.collectAsState().value,
        windowInsets = windowInsets,
    )
}