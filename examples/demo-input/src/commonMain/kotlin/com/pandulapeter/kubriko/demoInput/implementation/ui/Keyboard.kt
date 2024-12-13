package com.pandulapeter.kubriko.demoInput.implementation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.keyboardInput.extensions.displayName

@Composable
internal fun Keyboard(
    modifier: Modifier = Modifier,
    activeKeys: Set<Key>,
) = Box(
    modifier = Modifier.fillMaxSize(),
) {
    Column(
        modifier = modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KeyboardRow(
            keyWrappers = listOf(
                Key.Escape,
                Key.F1,
                Key.F2,
                Key.F3,
                Key.F4,
                Key.F5,
                Key.F6,
                Key.F7,
                Key.F8,
                Key.F9,
                Key.F10,
                Key.F11,
                Key.F12,
            ).map { it.toWrapper(activeKeys) },
        )
        KeyboardRow(
            keyWrappers = listOf(
                Key.Grave,
                Key.One,
                Key.Two,
                Key.Three,
                Key.Four,
                Key.Five,
                Key.Six,
                Key.Seven,
                Key.Eight,
                Key.Nine,
                Key.Zero,
                Key.Minus,
                Key.Equals,
                Key.Backspace,
            ).map { it.toWrapper(activeKeys) },
        )
        KeyboardRow(
            keyWrappers = listOf(
                Key.Tab,
                Key.Q,
                Key.W,
                Key.E,
                Key.R,
                Key.T,
                Key.Y,
                Key.U,
                Key.I,
                Key.O,
                Key.P,
                Key.LeftBracket,
                Key.RightBracket,
                Key.Backslash,
            ).map { it.toWrapper(activeKeys) },
        )
        KeyboardRow(
            keyWrappers = listOf(
                Key.CapsLock,
                Key.A,
                Key.S,
                Key.D,
                Key.F,
                Key.G,
                Key.H,
                Key.J,
                Key.K,
                Key.L,
                Key.Semicolon,
                Key.Apostrophe,
                Key.Enter,
            ).map { it.toWrapper(activeKeys) },
        )
        KeyboardRow(
            keyWrappers = listOf(
                Key.ShiftLeft,
                Key.Z,
                Key.X,
                Key.C,
                Key.V,
                Key.B,
                Key.N,
                Key.M,
                Key.Comma,
                Key.Period,
                Key.Slash,
                Key.ShiftRight,
            ).map { it.toWrapper(activeKeys) },
        )
        KeyboardRow(
            keyWrappers = listOf(
                Key.CtrlLeft,
                Key.AltLeft,
                Key.MetaLeft,
                Key.Spacebar,
                Key.MetaRight,
                Key.AltRight,
                Key.CtrlRight,
            ).map { it.toWrapper(activeKeys) },
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            textAlign = TextAlign.Center,
            text = activeKeys.joinToString { it.keyCode.toString() },
        )
    }
}

private fun Key.toWrapper(activeKeys: Set<Key>) = KeyWrapper(
    key = this,
    size = when (this) {
        Key.Escape, Key.Backspace, Key.Tab, Key.Enter, Key.CapsLock, Key.ShiftRight, Key.ShiftRight -> Size.WIDE
        Key.Spacebar -> Size.EXTRA_WIDE
        else -> Size.NORMAL
    },
    isPressed = this in activeKeys,
)

@Composable
private fun KeyboardRow(
    keyWrappers: List<KeyWrapper>,
) = Box {
    Row(
        modifier = Modifier.align(Alignment.Center),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keyWrappers.forEach { keyWrapper ->
            KeyButton(
                keyWrapper = keyWrapper,
            )
        }
    }
}

@Composable
private fun KeyButton(
    keyWrapper: KeyWrapper,
) = Box(
    modifier = Modifier
        .defaultMinSize(
            minWidth = when (keyWrapper.size) {
                Size.NORMAL -> 30.dp
                Size.WIDE -> 60.dp
                Size.EXTRA_WIDE -> 150.dp
            }
        )
        .height(30.dp)
        .background(
            if (keyWrapper.isPressed) {
                if (isSystemInDarkTheme()) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                }
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
        .padding(4.dp)
) {
    Text(
        modifier = Modifier.align(Alignment.Center),
        style = MaterialTheme.typography.bodySmall,
        text = keyWrapper.key.displayName,
        color = if (isSystemInDarkTheme() || !keyWrapper.isPressed) {
            Color.Unspecified
        } else {
            MaterialTheme.colorScheme.onPrimary
        }
    )
}

private data class KeyWrapper(
    val key: Key,
    val size: Size,
    val isPressed: Boolean,
)

private enum class Size {
    NORMAL,
    WIDE,
    EXTRA_WIDE,
}