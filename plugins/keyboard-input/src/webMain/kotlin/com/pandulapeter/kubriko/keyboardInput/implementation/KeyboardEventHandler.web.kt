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
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

@Composable
internal actual fun createKeyboardEventHandler(
    onKeyPressed: (Key) -> Unit,
    onKeyReleased: (Key) -> Unit,
    coroutineScope: CoroutineScope,
): KeyboardEventHandler = object : KeyboardEventHandler {
    private val pressedKeys = mutableSetOf<String>()
    private val keyDownListener: (Event) -> Unit = { event ->
        (event as? KeyboardEvent)?.lookupCode?.let { code ->
            val key = mapKeyboardEventCodeToKey(code)
            if (key != Key.Unknown && pressedKeys.add(code)) {
                onKeyPressed(key)
            }
        }
    }
    private val keyUpListener: (Event) -> Unit = { event ->
        (event as? KeyboardEvent)?.lookupCode?.let { code ->
            if (pressedKeys.remove(code)) {
                onKeyReleased(mapKeyboardEventCodeToKey(code))
            }
        }
    }

    /** The browser sends no keyup for a key released while the window is not focused. */
    private val blurListener: (Event) -> Unit = {
        pressedKeys.forEach { code -> onKeyReleased(mapKeyboardEventCodeToKey(code)) }
        pressedKeys.clear()
    }

    override fun startListening() {
        window.addEventListener("keydown", keyDownListener)
        window.addEventListener("keyup", keyUpListener)
        window.addEventListener("blur", blurListener)
    }

    override fun stopListening() {
        window.removeEventListener("keydown", keyDownListener)
        window.removeEventListener("keyup", keyUpListener)
        window.removeEventListener("blur", blurListener)
        pressedKeys.clear()
    }

    @Composable
    override fun isValid() = true
}

/** A virtual keyboard's events have an empty `code`, so Compose falls back to `key` for those, and so does this. */
private val KeyboardEvent.lookupCode get() = code.ifEmpty { key }

/**
 * Maps a `KeyboardEvent.code` (such as `"KeyA"` or `"ArrowLeft"`) to the [Key] constant Compose itself uses for it.
 *
 * Right-hand modifiers report their left-hand [Key] ([Key.ShiftLeft], [Key.CtrlLeft], [Key.AltLeft],
 * [Key.MetaLeft]), as on Desktop, where the two sides can't be told apart. Unknown codes give [Key.Unknown].
 */
fun mapKeyboardEventCodeToKey(code: String): Key = keyboardEventCodeToKey[code] ?: Key.Unknown

private val keyboardEventCodeToKey = mapOf(
    "KeyA" to Key.A,
    "KeyB" to Key.B,
    "KeyC" to Key.C,
    "KeyD" to Key.D,
    "KeyE" to Key.E,
    "KeyF" to Key.F,
    "KeyG" to Key.G,
    "KeyH" to Key.H,
    "KeyI" to Key.I,
    "KeyJ" to Key.J,
    "KeyK" to Key.K,
    "KeyL" to Key.L,
    "KeyM" to Key.M,
    "KeyN" to Key.N,
    "KeyO" to Key.O,
    "KeyP" to Key.P,
    "KeyQ" to Key.Q,
    "KeyR" to Key.R,
    "KeyS" to Key.S,
    "KeyT" to Key.T,
    "KeyU" to Key.U,
    "KeyV" to Key.V,
    "KeyW" to Key.W,
    "KeyX" to Key.X,
    "KeyY" to Key.Y,
    "KeyZ" to Key.Z,

    "Digit0" to Key.Zero,
    "Digit1" to Key.One,
    "Digit2" to Key.Two,
    "Digit3" to Key.Three,
    "Digit4" to Key.Four,
    "Digit5" to Key.Five,
    "Digit6" to Key.Six,
    "Digit7" to Key.Seven,
    "Digit8" to Key.Eight,
    "Digit9" to Key.Nine,

    "Numpad0" to Key.NumPad0,
    "Numpad1" to Key.NumPad1,
    "Numpad2" to Key.NumPad2,
    "Numpad3" to Key.NumPad3,
    "Numpad4" to Key.NumPad4,
    "Numpad5" to Key.NumPad5,
    "Numpad6" to Key.NumPad6,
    "Numpad7" to Key.NumPad7,
    "Numpad8" to Key.NumPad8,
    "Numpad9" to Key.NumPad9,

    "NumpadDivide" to Key.NumPadDivide,
    "NumpadMultiply" to Key.NumPadMultiply,
    "NumpadSubtract" to Key.NumPadSubtract,
    "NumpadAdd" to Key.NumPadAdd,
    "NumpadEnter" to Key.NumPadEnter,
    "NumpadEqual" to Key.NumPadEquals,
    "NumpadDecimal" to Key.NumPadDot,

    "NumLock" to Key.NumLock,

    "Minus" to Key.Minus,
    "Equal" to Key.Equals,
    "Backspace" to Key.Backspace,
    "BracketLeft" to Key.LeftBracket,
    "BracketRight" to Key.RightBracket,
    "Backslash" to Key.Backslash,
    "Semicolon" to Key.Semicolon,
    "Enter" to Key.Enter,
    "Comma" to Key.Comma,
    "Period" to Key.Period,
    "Slash" to Key.Slash,

    "ArrowLeft" to Key.DirectionLeft,
    "ArrowUp" to Key.DirectionUp,
    "ArrowRight" to Key.DirectionRight,
    "ArrowDown" to Key.DirectionDown,

    "Home" to Key.MoveHome,
    "PageUp" to Key.PageUp,
    "PageDown" to Key.PageDown,
    "Delete" to Key.Delete,
    "End" to Key.MoveEnd,

    "Backquote" to Key.Grave,
    "Tab" to Key.Tab,
    "CapsLock" to Key.CapsLock,
    "ScrollLock" to Key.ScrollLock,

    "ShiftLeft" to Key.ShiftLeft,
    "ControlLeft" to Key.CtrlLeft,
    "AltLeft" to Key.AltLeft,
    "MetaLeft" to Key.MetaLeft,

    "ShiftRight" to Key.ShiftLeft,
    "ControlRight" to Key.CtrlLeft,
    "AltRight" to Key.AltLeft,
    "MetaRight" to Key.MetaLeft,
    "Insert" to Key.Insert,

    "Escape" to Key.Escape,

    "F1" to Key.F1,
    "F2" to Key.F2,
    "F3" to Key.F3,
    "F4" to Key.F4,
    "F5" to Key.F5,
    "F6" to Key.F6,
    "F7" to Key.F7,
    "F8" to Key.F8,
    "F9" to Key.F9,
    "F10" to Key.F10,
    "F11" to Key.F11,
    "F12" to Key.F12,

    "Space" to Key.Spacebar,
    "Quote" to Key.Apostrophe,
)
