package com.pandulapeter.kubrikoShowcase.implementation.performance

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow

internal actual val sceneJson: MutableStateFlow<String>? = null

@Composable
internal actual fun BoxScope.PlatformSpecificContent() = Unit