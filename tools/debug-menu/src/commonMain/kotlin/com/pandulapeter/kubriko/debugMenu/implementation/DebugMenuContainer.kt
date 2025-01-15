package com.pandulapeter.kubriko.debugMenu.implementation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.debugMenu.implementation.ui.DebugMenuContents

@Composable
internal fun DebugMenuContainer(
    modifier: Modifier,
    windowInsets: WindowInsets,
    shouldUseVerticalLayout: Boolean,
    debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit,
) = debugMenuTheme {
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
            debugMenuMetadata = InternalDebugMenu.metadata.collectAsState(DebugMenuMetadata()).value,
            logs = InternalDebugMenu.logs.collectAsState(emptyList()).value,
            onIsDebugOverlayEnabledChanged = InternalDebugMenu::onIsDebugOverlayEnabledChanged,
            shouldUseVerticalLayout = shouldUseVerticalLayout,
        )
    }
}