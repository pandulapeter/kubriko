package com.pandulapeter.kubriko.shared.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingOverlay(
    modifier: Modifier = Modifier,
    shouldShowLoadingIndicator: Boolean,
) = AnimatedVisibility(
    visible = shouldShowLoadingIndicator,
    enter = fadeIn(animationSpec = tween(durationMillis = 0)),
    exit = fadeOut(animationSpec = tween(durationMillis = 1000)),
) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
    ) {
        CircularProgressIndicator(
            modifier = modifier.size(24.dp).align(Alignment.BottomStart),
            strokeWidth = 3.dp,
        )
    }
}