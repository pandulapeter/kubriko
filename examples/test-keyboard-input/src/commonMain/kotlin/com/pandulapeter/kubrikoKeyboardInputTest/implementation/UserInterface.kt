package com.pandulapeter.kubrikoKeyboardInputTest.implementation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun UserInterface(
    modifier: Modifier = Modifier,
    activeKeys: Set<Key>,
) = Box(
    modifier = modifier.fillMaxSize(),
) {

    val keySize = 40.dp
    val keyPadding = 4.dp

    Column(
        modifier = Modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Row 1: Function keys
        KeyboardRow(
            keys = listOf(Key.Escape, Key.F1, Key.F2, Key.F3, Key.F4, Key.F5, Key.F6, Key.F7, Key.F8, Key.F9, Key.F10, Key.F11, Key.F12),
            activeKeys = activeKeys,
            keySize = keySize,
            keyPadding = keyPadding
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Number row
        KeyboardRow(
            keys = listOf(Key.Grave, Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine, Key.Zero, Key.Minus, Key.Equals, Key.Backspace),
            activeKeys = activeKeys,
            keySize = keySize,
            keyPadding = keyPadding
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Row 3: QWERTY row
        KeyboardRow(
            keys = listOf(Key.Tab, Key.Q, Key.W, Key.E, Key.R, Key.T, Key.Y, Key.U, Key.I, Key.O, Key.P, Key.LeftBracket, Key.RightBracket, Key.Backslash),
            activeKeys = activeKeys,
            keySize = keySize,
            keyPadding = keyPadding
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Row 4: ASDF row
        KeyboardRow(
            keys = listOf(Key.CapsLock, Key.A, Key.S, Key.D, Key.F, Key.G, Key.H, Key.J, Key.K, Key.L, Key.Semicolon, Key.Apostrophe, Key.Enter),
            activeKeys = activeKeys,
            keySize = keySize,
            keyPadding = keyPadding
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Row 5: ZXCV row
        KeyboardRow(
            keys = listOf(Key.ShiftLeft, Key.Z, Key.X, Key.C, Key.V, Key.B, Key.N, Key.M, Key.Comma, Key.Period, Key.Slash, Key.ShiftRight),
            activeKeys = activeKeys,
            keySize = keySize,
            keyPadding = keyPadding
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Row 6: Bottom row (space bar and modifiers)
        KeyboardRow(
            keys = listOf(Key.CtrlLeft, Key.MetaLeft, Key.AltLeft, Key.Spacebar, Key.AltRight, Key.MetaRight, Key.Menu, Key.CtrlRight),
            activeKeys = activeKeys,
            keySize = keySize,
            keyPadding = keyPadding
        )
    }
}

@Composable
private fun KeyboardRow(
    keys: List<Key>,
    activeKeys: Set<Key>,
    keySize: Dp,
    keyPadding: Dp,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.forEach { key ->
            KeyButton(key = key, activeKeys = activeKeys, size = keySize, padding = keyPadding)
        }
    }
}

@Composable
private fun KeyButton(
    key: Key,
    activeKeys: Set<Key>,
    size: Dp,
    padding: Dp,
) {
    Box(
        modifier = Modifier
            .size(width = if (key == Key.Spacebar) size * 5 else size, height = size)
            .background(if (activeKeys.contains(key)) Color.White else Color.Gray)
            .padding(padding)
    ) {
        Text(
            text = key.toString().substringAfter(":"),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}