package com.pandulapeter.kubrikoShowcase.implementation.performance

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

@Composable
internal expect fun BoxScope.PlatformSpecificContent(
    onReloadClicked: () -> Unit,
)