/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.debugMenu.implementation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.debugMenu.implementation.ui.DebugMenuContents
import com.pandulapeter.kubriko.debugMenu.implementation.ui.LogsHeader
import com.pandulapeter.kubriko.logger.Logger

@Composable
internal fun DebugMenuContainer(
    modifier: Modifier,
    kubriko: Kubriko?,
    windowInsets: WindowInsets,
    shouldUseVerticalLayout: Boolean,
    debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit,
) = debugMenuTheme {
    DisposableEffect(kubriko) {
        kubriko?.let(InternalDebugMenu::registerGameKubriko)
        onDispose {
            kubriko?.let(InternalDebugMenu::unregisterGameKubriko)
        }
    }
    Surface(
        modifier = modifier,
        tonalElevation = when (isSystemInDarkTheme()) {
            true -> 4.dp
            false -> 0.dp
        },
        shadowElevation = when (isSystemInDarkTheme()) {
            true -> 4.dp
            false -> 2.dp
        },
    ) {
        DebugMenuContents(
            windowInsets = windowInsets,
            debugMenuMetadata = InternalDebugMenu.metadata.collectAsState().value,
            logs = InternalDebugMenu.logs.collectAsState(emptyList()).value,
            onIsBodyOverlayEnabledChanged = InternalDebugMenu::onIsBodyOverlayEnabledChanged,
            onIsCollisionMaskOverlayEnabledChanged = InternalDebugMenu::onIsCollisionMaskOverlayEnabledChanged,
            logsHeader = { modifier ->
                LogsHeader(
                    modifier = modifier,
                    isLowPriorityEnabled = InternalDebugMenu.isLowPriorityEnabled.collectAsState().value,
                    onLowPriorityToggled = InternalDebugMenu::onLowPriorityToggled,
                    isMediumPriorityEnabled = InternalDebugMenu.isMediumPriorityEnabled.collectAsState().value,
                    onMediumPriorityToggled = InternalDebugMenu::onMediumPriorityToggled,
                    isHighPriorityEnabled = InternalDebugMenu.isHighPriorityEnabled.collectAsState().value,
                    onHighPriorityToggled = InternalDebugMenu::onHighPriorityToggled,
                    isEditingFilter = InternalDebugMenu.isEditingFilter.collectAsState().value,
                    onEditingFilterToggled = InternalDebugMenu::toggleIsEditingFilter,
                    filterText = InternalDebugMenu.filter.collectAsState().value,
                    onFilterTextChanged = InternalDebugMenu::onFilterUpdated,
                    hasLogs = Logger.logs.collectAsState().value.isNotEmpty(),
                    onClearLogsClicked = Logger::clearLogs,
                )
            },
            shouldUseVerticalLayout = shouldUseVerticalLayout,
        )
    }
}