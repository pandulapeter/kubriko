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
import kotlin.test.assertNull

class HistoryShortcutsTest {

    @Test
    fun theShortcutKeyWithZUndoesAndWithShiftOrYRedoes() {
        assertEquals(true, historyStep(Key.Z, isShortcutPressed = true, isShiftPressed = false))
        assertEquals(false, historyStep(Key.Z, isShortcutPressed = true, isShiftPressed = true))
        assertEquals(false, historyStep(Key.Y, isShortcutPressed = true, isShiftPressed = false))
    }

    @Test
    fun otherKeysAndPlainTypingAreLeftAlone() {
        assertNull(historyStep(Key.Z, isShortcutPressed = false, isShiftPressed = false))
        assertNull(historyStep(Key.Y, isShortcutPressed = true, isShiftPressed = true))
        assertNull(historyStep(Key.A, isShortcutPressed = true, isShiftPressed = false))
    }
}
