package com.pandulapeter.kubriko.debugMenu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.debugMenu.implementation.DebugMenuContainer
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoTheme

@Composable
fun HorizontalDebugMenu(
    isVisible: Boolean = true,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit = { KubrikoTheme(it) },
) = AnimatedVisibility(
    visible = isVisible,
) {
    Box(
        modifier = Modifier.height(
            160.dp + windowInsets.asPaddingValues().calculateBottomPadding()
        ),
    ) {
        DebugMenuContainer(
            modifier = Modifier.fillMaxWidth(),
            windowInsets = windowInsets,
            shouldUseVerticalLayout = false,
            debugMenuTheme = debugMenuTheme,
        )
    }
}