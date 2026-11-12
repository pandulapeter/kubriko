/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals

class DoubleShiftTest {
    private var time = 0L
    private val doubleShift = DoubleShift { time }

    @Test
    fun shiftPressedTwiceQuicklyCountsOnTheSecondPressOfEitherShift() {
        assertEquals(false, press(Key.ShiftLeft))
        assertEquals(false, release(Key.ShiftLeft, after = 50))
        assertEquals(true, press(Key.ShiftRight, after = 100))
    }

    @Test
    fun aHeldShiftsRepeatedPressesAreOnePress() {
        press(Key.ShiftLeft)
        assertEquals(false, press(Key.ShiftLeft, after = 30))
        release(Key.ShiftLeft, after = 30)
        assertEquals(true, press(Key.ShiftLeft, after = 30))
    }

    @Test
    fun theSecondPressCountsOnlyWithinTheWindow() {
        press(Key.ShiftLeft)
        release(Key.ShiftLeft, after = 50)
        assertEquals(false, press(Key.ShiftLeft, after = 401))
        release(Key.ShiftLeft, after = 50)
        assertEquals(true, press(Key.ShiftLeft, after = 400))
    }

    @Test
    fun anotherKeyInBetweenOrWhileShiftIsHeldBreaksIt() {
        press(Key.ShiftLeft)
        release(Key.ShiftLeft, after = 50)
        press(Key.A, after = 10)
        assertEquals(false, press(Key.ShiftLeft, after = 10))
        press(Key.B, after = 10)
        release(Key.ShiftLeft, after = 10)
        assertEquals(false, press(Key.ShiftLeft, after = 10), "Shift held to type a capital doesn't count as a press of its own")
    }

    @Test
    fun aThirdPressStartsOver() {
        press(Key.ShiftLeft)
        release(Key.ShiftLeft, after = 10)
        assertEquals(true, press(Key.ShiftLeft, after = 10))
        release(Key.ShiftLeft, after = 10)
        assertEquals(false, press(Key.ShiftLeft, after = 10))
    }

    private fun press(key: Key, after: Long = 0): Boolean {
        time += after
        return doubleShift.onKey(key, isDown = true)
    }

    private fun release(key: Key, after: Long = 0): Boolean {
        time += after
        return doubleShift.onKey(key, isDown = false)
    }
}
