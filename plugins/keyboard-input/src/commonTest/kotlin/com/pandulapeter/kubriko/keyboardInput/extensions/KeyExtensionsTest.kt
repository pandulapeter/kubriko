/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.keyboardInput.extensions

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyExtensionsTest {

    @Test
    fun noKeysMeanNoDirection() {
        assertEquals(KeyboardDirectionState.NONE, emptySet<Key>().directionState)
    }

    @Test
    fun arrowKeysAndWasdMapToTheSameDirections() {
        mapOf(
            setOf(Key.DirectionLeft) to KeyboardDirectionState.LEFT,
            setOf(Key.A) to KeyboardDirectionState.LEFT,
            setOf(Key.DirectionUp) to KeyboardDirectionState.UP,
            setOf(Key.W) to KeyboardDirectionState.UP,
            setOf(Key.DirectionRight) to KeyboardDirectionState.RIGHT,
            setOf(Key.D) to KeyboardDirectionState.RIGHT,
            setOf(Key.DirectionDown) to KeyboardDirectionState.DOWN,
            setOf(Key.S) to KeyboardDirectionState.DOWN,
        ).forEach { (keys, direction) -> assertEquals(direction, keys.directionState, keys.toString()) }
    }

    @Test
    fun twoNeighbouringDirectionsCombineIntoADiagonal() {
        mapOf(
            setOf(Key.DirectionUp, Key.DirectionLeft) to KeyboardDirectionState.UP_LEFT,
            setOf(Key.W, Key.D) to KeyboardDirectionState.UP_RIGHT,
            setOf(Key.DirectionDown, Key.DirectionRight) to KeyboardDirectionState.DOWN_RIGHT,
            setOf(Key.S, Key.A) to KeyboardDirectionState.DOWN_LEFT,
        ).forEach { (keys, direction) -> assertEquals(direction, keys.directionState, keys.toString()) }
    }

    @Test
    fun opposingDirectionsCancelOut() {
        assertEquals(KeyboardDirectionState.NONE, setOf(Key.DirectionLeft, Key.DirectionRight).directionState)
        assertEquals(KeyboardDirectionState.NONE, setOf(Key.W, Key.S).directionState)
        assertEquals(KeyboardDirectionState.LEFT, setOf(Key.A, Key.W, Key.S).directionState)
        assertEquals(KeyboardDirectionState.UP, setOf(Key.W, Key.A, Key.D).directionState)
    }

    @Test
    fun keysWithoutADirectionAreIgnored() {
        assertEquals(KeyboardDirectionState.NONE, setOf(Key.Spacebar).directionState)
        assertEquals(KeyboardDirectionState.RIGHT, setOf(Key.D, Key.Spacebar).directionState)
    }

    @Test
    fun plusAndMinusKeysZoom() {
        listOf(Key.Plus, Key.Equals, Key.NumPadAdd, Key.ZoomIn).forEach {
            assertEquals(KeyboardZoomState.ZOOM_IN, setOf(it).zoomState, it.toString())
        }
        listOf(Key.Minus, Key.NumPadSubtract, Key.ZoomOut).forEach {
            assertEquals(KeyboardZoomState.ZOOM_OUT, setOf(it).zoomState, it.toString())
        }
    }

    @Test
    fun zoomingInAndOutTogetherCancelsOut() {
        assertEquals(KeyboardZoomState.NONE, setOf(Key.Plus, Key.Minus).zoomState)
        assertEquals(KeyboardZoomState.NONE, setOf(Key.A).zoomState)
        assertEquals(KeyboardZoomState.NONE, emptySet<Key>().zoomState)
    }

    @Test
    fun leftAndRightModifiersShareADisplayName() {
        assertEquals(Key.ShiftLeft.displayName, Key.ShiftRight.displayName)
        assertEquals(Key.CtrlLeft.displayName, Key.CtrlRight.displayName)
        assertEquals(Key.AltLeft.displayName, Key.AltRight.displayName)
        assertEquals(Key.MetaLeft.displayName, Key.MetaRight.displayName)
    }

    @Test
    fun digitKeysAndTheirNumPadCounterpartsShareADisplayName() {
        listOf(
            Key.Zero to Key.NumPad0,
            Key.One to Key.NumPad1,
            Key.Two to Key.NumPad2,
            Key.Three to Key.NumPad3,
            Key.Four to Key.NumPad4,
            Key.Five to Key.NumPad5,
            Key.Six to Key.NumPad6,
            Key.Seven to Key.NumPad7,
            Key.Eight to Key.NumPad8,
            Key.Nine to Key.NumPad9,
        ).forEachIndexed { digit, (key, numPadKey) ->
            assertEquals("$digit", key.displayName)
            assertEquals("$digit", numPadKey.displayName)
        }
    }
}
