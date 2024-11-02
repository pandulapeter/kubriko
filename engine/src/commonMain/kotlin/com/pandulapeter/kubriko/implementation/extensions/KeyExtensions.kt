package com.pandulapeter.kubriko.implementation.extensions

import androidx.compose.ui.input.key.Key

enum class KeyboardDirectionState {
    NONE, LEFT, UP_LEFT, UP, UP_RIGHT, RIGHT, DOWN_RIGHT, DOWN, DOWN_LEFT,
}

val Set<Key>.directionState
    get() = when {
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