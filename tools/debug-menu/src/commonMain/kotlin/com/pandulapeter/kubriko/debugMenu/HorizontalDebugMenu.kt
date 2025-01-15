package com.pandulapeter.kubriko.debugMenu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.debugMenu.implementation.DebugMenuContainer
import com.pandulapeter.kubriko.uiComponents.theme.KubrikoTheme

@Composable
fun HorizontalDebugMenu(
    modifier: Modifier = Modifier,
    kubriko: Kubriko?,
    isEnabled: Boolean = true,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    debugMenuTheme: @Composable (@Composable () -> Unit) -> Unit = { KubrikoTheme(it) },
    height: Dp = 160.dp,
) = AnimatedVisibility(
    visible = isEnabled && DebugMenu.isVisible.collectAsState().value,
    enter = expandIn(animationSpec = tween()) + fadeIn(),
    exit = fadeOut() + shrinkOut(animationSpec = tween()),
) {
    Box(
        modifier = modifier
            .height(height + windowInsets.asPaddingValues().calculateBottomPadding())
            .fillMaxWidth(),
    ) {
        DebugMenuContainer(
            modifier = Modifier.fillMaxWidth(),
            kubriko = kubriko,
            windowInsets = windowInsets,
            shouldUseVerticalLayout = false,
            debugMenuTheme = debugMenuTheme,
        )
    }
}