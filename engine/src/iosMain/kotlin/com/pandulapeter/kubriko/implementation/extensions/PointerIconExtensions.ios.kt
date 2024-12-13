package com.pandulapeter.kubriko.implementation.extensions

import androidx.compose.ui.input.pointer.DummyPointerIcon
import androidx.compose.ui.input.pointer.PointerIcon

// TODO: Doesn't work because BrowserCursor is internal to Compose
internal actual val pointerIconInvisible: PointerIcon = DummyPointerIcon