package com.pandulapeter.kubriko.debugMenu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.debugMenu.implementation.DebugMenuContainer
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoTheme

@Composable
fun VerticalDebugMenu(
    modifier: Modifier = Modifier,
    kubriko: Kubriko?,
    isEnabled: Boolean = true,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit = { KubrikoTheme(it) },
    width: Dp = 180.dp,
) = AnimatedVisibility(
    visible = isEnabled && DebugMenu.isVisible.collectAsState().value,
    enter = expandIn(animationSpec = tween()) + fadeIn(),
    exit = fadeOut() + shrinkOut(animationSpec = tween()),
) {
    DebugMenuContainer(
        modifier = modifier
            .width(width + windowInsets.only(WindowInsetsSides.Right).asPaddingValues().calculateRightPadding(LocalLayoutDirection.current))
            .fillMaxHeight(),
        kubriko = kubriko,
        windowInsets = windowInsets,
        shouldUseVerticalLayout = true,
        debugMenuTheme = debugMenuTheme,
    )
}