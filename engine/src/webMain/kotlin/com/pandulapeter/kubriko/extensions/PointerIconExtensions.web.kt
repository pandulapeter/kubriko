package com.pandulapeter.kubriko.extensions

import androidx.compose.ui.input.pointer.PointerIcon

// Can't implement invisible cursor because DarwinCursor is private to Compose
internal actual val pointerIconInvisible: PointerIcon = PointerIcon.Crosshair