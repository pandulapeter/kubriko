package com.pandulapeter.kubriko.implementation.extensions

import androidx.compose.ui.input.pointer.DummyPointerIcon
import androidx.compose.ui.input.pointer.PointerIcon

// TODO: Doesn't work because DarwinCursor is private to Compose
internal actual val pointerIconInvisible: PointerIcon = DummyPointerIcon