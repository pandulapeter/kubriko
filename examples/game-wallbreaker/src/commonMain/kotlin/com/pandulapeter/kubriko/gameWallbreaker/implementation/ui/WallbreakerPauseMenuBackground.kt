package com.pandulapeter.kubriko.gameWallbreaker.implementation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
internal fun WallbreakerPauseMenuBackground(
    isVisible: Boolean,
) = AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn(),
    exit = fadeOut(),
) {
    Box(
        modifier = Modifier.fillMaxSize().background(color = Color.Black.copy(0.8f)),
    )
}