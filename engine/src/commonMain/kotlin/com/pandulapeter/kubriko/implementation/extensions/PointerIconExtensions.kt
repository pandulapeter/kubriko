package com.pandulapeter.kubriko.implementation.extensions

import androidx.compose.ui.input.pointer.PointerIcon

val PointerIcon.Companion.Invisible get() = pointerIconInvisible

// TODO: Only works on Desktop
internal expect val pointerIconInvisible: PointerIcon