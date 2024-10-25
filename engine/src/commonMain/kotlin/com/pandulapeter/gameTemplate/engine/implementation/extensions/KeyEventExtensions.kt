package com.pandulapeter.gameTemplate.engine.implementation.extensions

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.pandulapeter.gameTemplate.engine.implementation.utilities.consume

fun KeyEvent.proccess(
    addToActiveKeys: (Key) -> Unit,
    removeFromActiveKeys: (Key) -> Unit,
    onKeyRelease: (Key) -> Unit,
) = consume {
    if (type == KeyEventType.KeyDown) {
        addToActiveKeys(key)
    }
    if (type == KeyEventType.KeyUp) {
        removeFromActiveKeys(key)
        onKeyRelease(key)
    }
}