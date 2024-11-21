package com.pandulapeter.kubrikoShowcase.implementation.performance

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

@Composable
internal actual fun BoxScope.PlatformSpecificContent() = Unit

internal actual val sceneEditorRealtimeContent : StateFlow<String>? = null