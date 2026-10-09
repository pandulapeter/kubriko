/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testInput.implementation.ui

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyboardTest {

    @Test
    fun bothShiftKeysAreWide() {
        assertEquals(Size.WIDE, Key.ShiftLeft.keySize())
        assertEquals(Size.WIDE, Key.ShiftRight.keySize())
    }
}
