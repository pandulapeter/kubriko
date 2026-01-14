/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.keyboardInput.implementation

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import kotlinx.coroutines.CoroutineScope
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.KeyEvent

@Composable
internal actual fun createKeyboardEventHandler(
    onKeyPressed: (Key) -> Unit,
    onKeyReleased: (Key) -> Unit,
    coroutineScope: CoroutineScope,
): KeyboardEventHandler = object : KeyboardEventHandler, AWTEventListener {

    override fun startListening(
    ) = Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK)

    override fun stopListening() = Toolkit.getDefaultToolkit().removeAWTEventListener(this)

    @Composable
    override fun isValid() = true

    override fun eventDispatched(event: AWTEvent?) {
        if (event is KeyEvent) {
            event.toKey()?.let { key ->
                when (event.id) {
                    KeyEvent.KEY_PRESSED -> onKeyPressed(key)
                    KeyEvent.KEY_RELEASED -> onKeyReleased(key)
                }
            }
        }
    }
}

private fun KeyEvent.toKey() = keyMap[keyCode]

private val keyMap = mapOf(
    KeyEvent.VK_A to Key.A,
    KeyEvent.VK_B to Key.B,
    KeyEvent.VK_C to Key.C,
    KeyEvent.VK_D to Key.D,
    KeyEvent.VK_E to Key.E,
    KeyEvent.VK_F to Key.F,
    KeyEvent.VK_G to Key.G,
    KeyEvent.VK_H to Key.H,
    KeyEvent.VK_I to Key.I,
    KeyEvent.VK_J to Key.J,
    KeyEvent.VK_K to Key.K,
    KeyEvent.VK_L to Key.L,
    KeyEvent.VK_M to Key.M,
    KeyEvent.VK_N to Key.N,
    KeyEvent.VK_O to Key.O,
    KeyEvent.VK_P to Key.P,
    KeyEvent.VK_Q to Key.Q,
    KeyEvent.VK_R to Key.R,
    KeyEvent.VK_S to Key.S,
    KeyEvent.VK_T to Key.T,
    KeyEvent.VK_U to Key.U,
    KeyEvent.VK_V to Key.V,
    KeyEvent.VK_W to Key.W,
    KeyEvent.VK_X to Key.X,
    KeyEvent.VK_Y to Key.Y,
    KeyEvent.VK_Z to Key.Z,

    KeyEvent.VK_0 to Key.Zero,
    KeyEvent.VK_1 to Key.One,
    KeyEvent.VK_2 to Key.Two,
    KeyEvent.VK_3 to Key.Three,
    KeyEvent.VK_4 to Key.Four,
    KeyEvent.VK_5 to Key.Five,
    KeyEvent.VK_6 to Key.Six,
    KeyEvent.VK_7 to Key.Seven,
    KeyEvent.VK_8 to Key.Eight,
    KeyEvent.VK_9 to Key.Nine,

    KeyEvent.VK_ENTER to Key.Enter,
    KeyEvent.VK_ESCAPE to Key.Escape,
    KeyEvent.VK_BACK_SPACE to Key.Backspace,
    KeyEvent.VK_TAB to Key.Tab,
    KeyEvent.VK_SPACE to Key.Spacebar,
    KeyEvent.VK_MINUS to Key.Minus,
    KeyEvent.VK_EQUALS to Key.Equals,
    KeyEvent.VK_OPEN_BRACKET to Key.LeftBracket,
    KeyEvent.VK_CLOSE_BRACKET to Key.RightBracket,
    KeyEvent.VK_BACK_SLASH to Key.Backslash,
    KeyEvent.VK_SEMICOLON to Key.Semicolon,
    KeyEvent.VK_QUOTE to Key.Apostrophe,
    KeyEvent.VK_COMMA to Key.Comma,
    KeyEvent.VK_PERIOD to Key.Period,
    KeyEvent.VK_SLASH to Key.Slash,

    KeyEvent.VK_F1 to Key.F1,
    KeyEvent.VK_F2 to Key.F2,
    KeyEvent.VK_F3 to Key.F3,
    KeyEvent.VK_F4 to Key.F4,
    KeyEvent.VK_F5 to Key.F5,
    KeyEvent.VK_F6 to Key.F6,
    KeyEvent.VK_F7 to Key.F7,
    KeyEvent.VK_F8 to Key.F8,
    KeyEvent.VK_F9 to Key.F9,
    KeyEvent.VK_F10 to Key.F10,
    KeyEvent.VK_F11 to Key.F11,
    KeyEvent.VK_F12 to Key.F12,

    KeyEvent.VK_SHIFT to Key.ShiftLeft, // Left Shift, as Java doesn’t differentiate
    KeyEvent.VK_CONTROL to Key.CtrlLeft, // Left Ctrl
    KeyEvent.VK_ALT to Key.AltLeft, // Left Alt
    KeyEvent.VK_META to Key.MetaLeft, // Left Meta (usually Command on macOS)

    KeyEvent.VK_CAPS_LOCK to Key.CapsLock,
    KeyEvent.VK_NUM_LOCK to Key.NumLock,
    KeyEvent.VK_SCROLL_LOCK to Key.ScrollLock,
    KeyEvent.VK_INSERT to Key.Insert,
    KeyEvent.VK_DELETE to Key.Delete,
    KeyEvent.VK_HOME to Key.MoveHome,
    KeyEvent.VK_END to Key.MoveEnd,
    KeyEvent.VK_PAGE_UP to Key.PageUp,
    KeyEvent.VK_PAGE_DOWN to Key.PageDown,
    KeyEvent.VK_UP to Key.DirectionUp,
    KeyEvent.VK_DOWN to Key.DirectionDown,
    KeyEvent.VK_LEFT to Key.DirectionLeft,
    KeyEvent.VK_RIGHT to Key.DirectionRight,

    KeyEvent.VK_NUMPAD0 to Key.NumPad0,
    KeyEvent.VK_NUMPAD1 to Key.NumPad1,
    KeyEvent.VK_NUMPAD2 to Key.NumPad2,
    KeyEvent.VK_NUMPAD3 to Key.NumPad3,
    KeyEvent.VK_NUMPAD4 to Key.NumPad4,
    KeyEvent.VK_NUMPAD5 to Key.NumPad5,
    KeyEvent.VK_NUMPAD6 to Key.NumPad6,
    KeyEvent.VK_NUMPAD7 to Key.NumPad7,
    KeyEvent.VK_NUMPAD8 to Key.NumPad8,
    KeyEvent.VK_NUMPAD9 to Key.NumPad9,
    KeyEvent.VK_MULTIPLY to Key.NumPadMultiply,
    KeyEvent.VK_ADD to Key.NumPadAdd,
    KeyEvent.VK_SUBTRACT to Key.NumPadSubtract,
    KeyEvent.VK_DECIMAL to Key.NumPadDot,
    KeyEvent.VK_DIVIDE to Key.NumPadDivide
)