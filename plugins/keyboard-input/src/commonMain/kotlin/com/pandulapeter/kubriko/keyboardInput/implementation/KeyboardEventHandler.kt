package com.pandulapeter.kubriko.keyboardInput.implementation

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import kotlinx.coroutines.CoroutineScope

internal interface KeyboardEventHandler {

    @Composable
    fun isValid(): Boolean

    fun startListening()

    fun stopListening()
}

@Composable
internal expect fun createKeyboardEventHandler(
    onKeyPressed: (Key) -> Unit,
    onKeyReleased: (Key) -> Unit,
    coroutineScope: CoroutineScope,
): KeyboardEventHandler