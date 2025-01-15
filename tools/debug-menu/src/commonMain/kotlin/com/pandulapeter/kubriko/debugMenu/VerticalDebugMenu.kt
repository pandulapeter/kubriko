package com.pandulapeter.kubriko.debugMenu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.debugMenu.implementation.DebugMenuContainer
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoTheme

@Composable
fun VerticalDebugMenu(
    isVisible: Boolean = true,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit = { KubrikoTheme(it) },
) = AnimatedVisibility(
    visible = isVisible,
) {
    DebugMenuContainer(
        modifier = Modifier
            .defaultMinSize(
                minWidth = 180.dp + windowInsets.only(WindowInsetsSides.Right).asPaddingValues()
                    .calculateRightPadding(LocalLayoutDirection.current)
            ).fillMaxHeight(),
        windowInsets = windowInsets,
        shouldUseVerticalLayout = true,
        debugMenuTheme = debugMenuTheme,
    )
}