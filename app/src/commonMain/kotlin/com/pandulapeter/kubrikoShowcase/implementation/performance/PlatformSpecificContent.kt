package com.pandulapeter.kubrikoShowcase.implementation.performance

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

@Composable
internal expect fun BoxScope.PlatformSpecificContent()

internal expect val sceneEditorRealtimeContent: StateFlow<String>?