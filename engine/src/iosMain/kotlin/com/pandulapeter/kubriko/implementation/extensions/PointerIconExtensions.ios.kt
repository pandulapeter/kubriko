package com.pandulapeter.kubriko.implementation.extensions

import androidx.compose.ui.input.pointer.PointerIcon

// Can't implement invisible cursor because BrowserCursor is internal to Compose
internal actual val pointerIconInvisible: PointerIcon = PointerIcon.Default