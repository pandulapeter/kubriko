/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
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
    private val pressedKeys = mutableSetOf<String>() // Track keys that are currently pressed
    private val keyDownListener: (Event) -> Unit = { event ->
        (event as? KeyboardEvent)?.let { keyboardEvent ->
            if (pressedKeys.add(keyboardEvent.code)) {
                onKeyPressed(mapKeyboardEventCodeToKey(keyboardEvent.code))
            }
        }
    }
    private val keyUpListener: (Event) -> Unit = { event ->
        (event as? KeyboardEvent)?.let { keyboardEvent ->
            if (pressedKeys.remove(keyboardEvent.code)) {
                onKeyReleased(mapKeyboardEventCodeToKey(keyboardEvent.code))
            }
        }
    }

    override fun startListening() {
        window.addEventListener("keydown", keyDownListener)
        window.addEventListener("keyup", keyUpListener)
    }

    override fun stopListening() {
        window.removeEventListener("keydown", keyDownListener)
        window.removeEventListener("keyup", keyUpListener)
        pressedKeys.clear()
    }

    @Composable
    override fun isValid() = true
}

fun mapKeyboardEventCodeToKey(code: String): Key {
    return when (code) {
        "KeyA" -> Key(65)      // A
        "KeyB" -> Key(66)      // B
        "KeyC" -> Key(67)      // C
        "KeyD" -> Key(68)      // D
        "KeyE" -> Key(69)      // E
        "KeyF" -> Key(70)      // F
        "KeyG" -> Key(71)      // G
        "KeyH" -> Key(72)      // H
        "KeyI" -> Key(73)      // I
        "KeyJ" -> Key(74)      // J
        "KeyK" -> Key(75)      // K
        "KeyL" -> Key(76)      // L
        "KeyM" -> Key(77)      // M
        "KeyN" -> Key(78)      // N
        "KeyO" -> Key(79)      // O
        "KeyP" -> Key(80)      // P
        "KeyQ" -> Key(81)      // Q
        "KeyR" -> Key(82)      // R
        "KeyS" -> Key(83)      // S
        "KeyT" -> Key(84)      // T
        "KeyU" -> Key(85)      // U
        "KeyV" -> Key(86)      // V
        "KeyW" -> Key(87)      // W
        "KeyX" -> Key(88)      // X
        "KeyY" -> Key(89)      // Y
        "KeyZ" -> Key(90)      // Z
        "Digit0" -> Key(48)    // 0
        "Digit1" -> Key(49)    // 1
        "Digit2" -> Key(50)    // 2
        "Digit3" -> Key(51)    // 3
        "Digit4" -> Key(52)    // 4
        "Digit5" -> Key(53)    // 5
        "Digit6" -> Key(54)    // 6
        "Digit7" -> Key(55)    // 7
        "Digit8" -> Key(56)    // 8
        "Digit9" -> Key(57)    // 9
        "Enter" -> Key(13)     // Enter
        "Escape" -> Key(27)    // Escape
        "Space" -> Key(32)     // Space
        "ArrowLeft" -> Key(37) // Left Arrow
        "ArrowUp" -> Key(38)   // Up Arrow
        "ArrowRight" -> Key(39) // Right Arrow
        "ArrowDown" -> Key(40) // Down Arrow
        "Backspace" -> Key(8)  // Backspace
        "Tab" -> Key(9)        // Tab
        "ShiftLeft", "ShiftRight" -> Key(16) // Shift
        "ControlLeft", "ControlRight" -> Key(17) // Control
        "AltLeft", "AltRight" -> Key(18) // Alt
        "MetaLeft", "MetaRight" -> Key(91) // Meta (Windows/Cmd)
        "Delete" -> Key(46)    // Delete
        else -> Key(-1)        // Unknown
    }
}