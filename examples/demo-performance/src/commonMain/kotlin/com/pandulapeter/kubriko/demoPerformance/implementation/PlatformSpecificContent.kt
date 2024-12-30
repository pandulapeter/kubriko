package com.pandulapeter.kubriko.demoPerformance.implementation

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow

internal expect val sceneJson: MutableStateFlow<String>?

@Composable
internal expect fun PlatformSpecificContent()