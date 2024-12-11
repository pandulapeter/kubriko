package com.pandulapeter.kubriko.keyboardInput.implementation

import androidx.compose.ui.input.key.Key
import kotlinx.coroutines.CoroutineScope

internal interface KeyboardEventHandler {

    fun startListening()

    fun stopListening()
}

internal expect fun createKeyboardEventHandler(
    onKeyPressed: (Key) -> Unit,
    onKeyReleased: (Key) -> Unit,
    coroutineScope: CoroutineScope,
): KeyboardEventHandler