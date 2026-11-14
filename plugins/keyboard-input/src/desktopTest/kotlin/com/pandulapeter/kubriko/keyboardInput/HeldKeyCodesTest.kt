/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.keyboardInput

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeldKeyCodesTest {

    @Test
    fun keysAreHeldUntilRemovedOrClearedEvenPastTheFirstCapacity() {
        val keys = listOf(Key.A, Key.B, Key.C, Key.D, Key.E, Key.F, Key.G, Key.H, Key.I, Key.J)
        val held = HeldKeyCodes()
        keys.forEach(held::add)
        held.add(Key.A)
        assertTrue(keys.all { it in held })
        held.remove(Key.A)
        held.remove(Key.A)
        assertFalse(Key.A in held)
        assertTrue((keys - Key.A).all { it in held })
        held.remove(Key.Z)
        assertTrue((keys - Key.A).all { it in held })
        held.clear()
        assertTrue(keys.none { it in held })
    }
}
