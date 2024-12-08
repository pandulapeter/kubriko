package com.pandulapeter.kubriko.demoPhysics.implementation

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow

internal expect val sceneJson: MutableStateFlow<String>?

@Composable
internal expect fun BoxScope.PlatformSpecificContent()