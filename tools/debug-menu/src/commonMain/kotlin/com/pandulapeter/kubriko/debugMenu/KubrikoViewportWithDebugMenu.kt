package com.pandulapeter.kubriko.debugMenu

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.debugMenu.implementation.InternalDebugMenu
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoTheme

/**
 * TODO: Documentation
 */
@Composable
fun KubrikoViewportWithDebugMenu(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    kubriko: Kubriko?,
    isEnabled: Boolean = true,
    buttonAlignment: Alignment? = Alignment.TopStart,
    debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit = { KubrikoTheme(it) },
    kubrikoViewport: @Composable BoxScope.() -> Unit,
) = BoxWithConstraints(
    modifier = modifier,
) {
    LaunchedEffect(kubriko) {
        InternalDebugMenu.setGameKubriko(kubriko)
    }
    if (kubriko != null && isEnabled) {
        val isVisible = InternalDebugMenu.isVisible.collectAsState().value
        val isColumn = maxWidth < maxHeight
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                modifier = Modifier.weight(1f),
            ) {
                KubrikoViewportWithDebugMenuOverlay(
                    modifier = Modifier.weight(1f),
                    kubrikoViewport = kubrikoViewport,
                    buttonAlignment = buttonAlignment,
                    windowInsets = windowInsets,
                    isVisible = isVisible,
                )
                VerticalDebugMenu(
                    kubriko = kubriko,
                    isVisible = isVisible && !isColumn,
                    windowInsets = windowInsets,
                    debugMenuTheme = debugMenuTheme,
                )
            }
            HorizontalDebugMenu(
                kubriko = kubriko,
                isVisible = isVisible && isColumn,
                windowInsets = windowInsets,
                debugMenuTheme = debugMenuTheme,
            )
        }
    } else {
        kubrikoViewport()
    }
}