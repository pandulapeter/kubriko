package com.pandulapeter.gameTemplate.engine.implementation.helpers

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.KeyEvent

@Composable
internal actual fun rememberKeyboardEventHandler(
    onKeyPressed: (Key) -> Unit,
    onKeyReleased: (Key) -> Unit
): KeyboardEventHandler = object : KeyboardEventHandler, AWTEventListener {


    override fun startListening(
    ) = Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK)

    override fun stopListening() = Toolkit.getDefaultToolkit().removeAWTEventListener(this)

    override fun eventDispatched(event: AWTEvent?) {
        if (event is KeyEvent) {
            when (event.id) {
                KeyEvent.KEY_PRESSED -> onKeyPressed(Key(event.keyCode))
                KeyEvent.KEY_RELEASED -> onKeyReleased(Key(event.keyCode))
            }
        }
    }
}