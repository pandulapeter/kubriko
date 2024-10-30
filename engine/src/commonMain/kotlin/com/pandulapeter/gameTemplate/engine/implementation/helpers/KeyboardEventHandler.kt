package com.pandulapeter.gameTemplate.engine.implementation.helpers

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key

internal interface KeyboardEventHandler {

    fun startListening()

    fun stopListening()
}

@Composable
internal expect fun rememberKeyboardEventHandler(
    onKeyPressed: (Key) -> Unit,
    onKeyReleased: (Key) -> Unit
): KeyboardEventHandler