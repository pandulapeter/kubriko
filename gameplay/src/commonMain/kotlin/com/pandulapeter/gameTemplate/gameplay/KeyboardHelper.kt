package com.pandulapeter.gameTemplate.gameplay

import androidx.compose.ui.input.key.Key

internal enum class KeyboardDirection {
    NONE, LEFT, UP_LEFT, UP, UP_RIGHT, RIGHT, DOWN_RIGHT, DOWN, DOWN_LEFT,
}

internal val Set<Key>.direction
    get() = when {
        hasLeft && !hasRight && !hasUp && !hasDown -> KeyboardDirection.LEFT
        hasUpLeft && !hasDown && !hasRight -> KeyboardDirection.UP_LEFT
        hasUp && !hasDown && !hasLeft && !hasRight -> KeyboardDirection.UP
        hasUpRight && !hasDown && !hasLeft -> KeyboardDirection.UP_RIGHT
        hasRight && !hasLeft && !hasUp && !hasDown -> KeyboardDirection.RIGHT
        hasDownRight && !hasUp && !hasLeft -> KeyboardDirection.DOWN_RIGHT
        hasDown && !hasUp && !hasLeft && !hasRight -> KeyboardDirection.DOWN
        hasDownLeft && !hasUp && !hasRight -> KeyboardDirection.DOWN_LEFT
        else -> KeyboardDirection.NONE
    }

private val Set<Key>.hasLeft get() = contains(Key.DirectionLeft) || contains(Key.A)

private val Set<Key>.hasUpLeft get() = contains(Key.DirectionUpLeft) || (contains(Key.DirectionUp) && contains(Key.DirectionLeft)) || ((contains(Key.W) && contains(Key.A)))

private val Set<Key>.hasUp get() = contains(Key.DirectionUp) || contains(Key.W)

private val Set<Key>.hasUpRight get() = contains(Key.DirectionUpRight) || (contains(Key.DirectionUp) && contains(Key.DirectionRight)) || ((contains(Key.W) && contains(Key.D)))

private val Set<Key>.hasRight get() = contains(Key.DirectionRight) || contains(Key.D)

private val Set<Key>.hasDownRight get() = contains(Key.DirectionDownRight) || (contains(Key.DirectionDown) && contains(Key.DirectionRight)) || ((contains(Key.S) && contains(Key.D)))

private val Set<Key>.hasDown get() = contains(Key.DirectionDown) || contains(Key.S)

private val Set<Key>.hasDownLeft get() = contains(Key.DirectionDownLeft) || (contains(Key.DirectionDown) && contains(Key.DirectionLeft)) || ((contains(Key.S) && contains(Key.A)))