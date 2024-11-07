package com.pandulapeter.kubriko.keyboardInputManager.extensions

import androidx.compose.ui.input.key.Key

enum class KeyboardDirectionState {
    NONE, LEFT, UP_LEFT, UP, UP_RIGHT, RIGHT, DOWN_RIGHT, DOWN, DOWN_LEFT,
}

// TODO: Should be optimized, rewrite using vectors
val Set<Key>.directionState
    get() = when {
        isEmpty() -> KeyboardDirectionState.NONE
        (hasLeft && !hasRight && !hasUp && !hasDown) || (hasLeft && !hasRight && hasUp && hasDown) -> KeyboardDirectionState.LEFT
        hasUpLeft && !hasDown && !hasRight -> KeyboardDirectionState.UP_LEFT
        (hasUp && !hasDown && !hasLeft && !hasRight) || (hasUp && !hasDown && hasLeft && hasRight) -> KeyboardDirectionState.UP
        hasUpRight && !hasDown && !hasLeft -> KeyboardDirectionState.UP_RIGHT
        (hasRight && !hasLeft && !hasUp && !hasDown) || (hasRight && !hasLeft && hasUp && hasDown) -> KeyboardDirectionState.RIGHT
        hasDownRight && !hasUp && !hasLeft -> KeyboardDirectionState.DOWN_RIGHT
        (hasDown && !hasUp && !hasLeft && !hasRight) || (hasDown && !hasUp && hasLeft && hasRight) -> KeyboardDirectionState.DOWN
        hasDownLeft && !hasUp && !hasRight -> KeyboardDirectionState.DOWN_LEFT
        else -> KeyboardDirectionState.NONE
    }

enum class KeyboardZoomState {
    NONE, ZOOM_IN, ZOOM_OUT,
}

val Set<Key>.zoomState
    get() = when {
        isEmpty() -> KeyboardZoomState.NONE
        hasZoomIn && !hasZoomOut -> KeyboardZoomState.ZOOM_IN
        hasZoomOut && !hasZoomIn -> KeyboardZoomState.ZOOM_OUT
        else -> KeyboardZoomState.NONE
    }

private val Set<Key>.hasLeft get() = contains(Key.DirectionLeft) || contains(Key.A)

private val Set<Key>.hasUpLeft get() = contains(Key.DirectionUpLeft) || (contains(Key.DirectionUp) && contains(Key.DirectionLeft)) || ((contains(Key.W) && contains(Key.A)))

private val Set<Key>.hasUp get() = contains(Key.DirectionUp) || contains(Key.W)

private val Set<Key>.hasUpRight get() = contains(Key.DirectionUpRight) || (contains(Key.DirectionUp) && contains(Key.DirectionRight)) || ((contains(Key.W) && contains(Key.D)))

private val Set<Key>.hasRight get() = contains(Key.DirectionRight) || contains(Key.D)

private val Set<Key>.hasDownRight get() = contains(Key.DirectionDownRight) || (contains(Key.DirectionDown) && contains(Key.DirectionRight)) || ((contains(Key.S) && contains(Key.D)))

private val Set<Key>.hasDown get() = contains(Key.DirectionDown) || contains(Key.S)

private val Set<Key>.hasDownLeft get() = contains(Key.DirectionDownLeft) || (contains(Key.DirectionDown) && contains(Key.DirectionLeft)) || ((contains(Key.S) && contains(Key.A)))

private val Set<Key>.hasZoomIn get() = contains(Key.Plus) || contains(Key.Equals) || contains(Key.NumPadAdd) || contains(Key.ZoomIn)

private val Set<Key>.hasZoomOut get() = contains(Key.Minus) || contains(Key.NumPadSubtract) || contains(Key.ZoomOut)

// TODO
val Key.displayName
    get() = when (this) {
        Key.Escape -> "Esc"
        Key.F1 -> "F1"
        Key.F2 -> "F2"
        Key.F3 -> "F3"
        Key.F4 -> "F4"
        Key.F5 -> "F5"
        Key.F6 -> "F6"
        Key.F7 -> "F7"
        Key.F8 -> "F8"
        Key.F9 -> "F9"
        Key.F10 -> "F10"
        Key.F11 -> "F11"
        Key.F12 -> "F12"
        Key.Grave -> "`"
        Key.One, Key.NumPad1 -> "1"
        Key.Two, Key.NumPad2 -> "2"
        Key.Three, Key.NumPad3 -> "3"
        Key.Four, Key.NumPad4 -> "4"
        Key.Five, Key.NumPad5 -> "5"
        Key.Six, Key.NumPad6 -> "6"
        Key.Seven, Key.NumPad7 -> "7"
        Key.Eight, Key.NumPad8 -> "8"
        Key.Nine, Key.Nine-> "9"
        Key.Zero, Key.NumPad0 -> "0"
        Key.Minus -> "-"
        Key.Plus -> "+"
        Key.Equals, Key.NumPadEquals -> "="
        Key.Backspace -> "Backspace"
        Key.Tab -> "Tab"
        Key.Enter, Key.NumPadEnter -> "Enter"
        Key.CapsLock -> "CapsLock"
        Key.ShiftRight, Key.ShiftLeft -> "Shift"
        Key.Function -> "Fn"
        Key.CtrlLeft, Key.CtrlRight -> "Ctrl"
        Key.MetaLeft, Key.MetaRight -> "Meta"
        Key.AltLeft, Key.AltRight -> "Alt"
        Key.Spacebar -> "Spacebar"
        Key.LeftBracket -> "["
        Key.RightBracket -> "]"
        Key.Backslash -> "\\"
        Key.Slash -> "/"
        Key.Semicolon -> ";"
        Key.Apostrophe-> "'"
        Key.Comma-> ","
        Key.Period-> "."
        Key.Q-> "Q"
        Key.W-> "W"
        Key.E-> "E"
        Key.R-> "R"
        Key.T-> "T"
        Key.Y-> "Y"
        Key.U-> "U"
        Key.I-> "I"
        Key.O-> "O"
        Key.P-> "P"
        Key.A-> "A"
        Key.S-> "S"
        Key.D-> "D"
        Key.F-> "F"
        Key.G-> "G"
        Key.H-> "H"
        Key.J-> "J"
        Key.K-> "K"
        Key.L-> "L"
        Key.Z-> "Z"
        Key.X-> "X"
        Key.C-> "C"
        Key.V-> "V"
        Key.B-> "B"
        Key.N-> "N"
        Key.M-> "M"
        else -> toString().substringAfter(":")
    }