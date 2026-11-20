/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SceneKeyActionTest {

    @Test
    fun theArrowsMoveByAStepAndWithShiftByTen() {
        assertEquals(SceneKeyAction.Nudge(-1f, 0f), SceneKeyAction.of(Key.DirectionLeft, isShiftPressed = false, isCommandPressed = false, step = 1f))
        assertEquals(SceneKeyAction.Nudge(0f, 160f), SceneKeyAction.of(Key.DirectionDown, isShiftPressed = true, isCommandPressed = false, step = 16f))
        assertNull(SceneKeyAction.of(Key.DirectionUp, isShiftPressed = false, isCommandPressed = true, step = 1f))
    }

    @Test
    fun theOtherKeysRemoveDuplicateAndSelect() {
        assertEquals(SceneKeyAction.Remove, SceneKeyAction.of(Key.Backspace, isShiftPressed = false, isCommandPressed = false, step = 1f))
        assertEquals(SceneKeyAction.Duplicate, SceneKeyAction.of(Key.D, isShiftPressed = false, isCommandPressed = true, step = 1f))
        assertEquals(SceneKeyAction.SelectAll, SceneKeyAction.of(Key.A, isShiftPressed = false, isCommandPressed = true, step = 1f))
        assertEquals(SceneKeyAction.ClearSelection, SceneKeyAction.of(Key.Escape, isShiftPressed = false, isCommandPressed = false, step = 1f))
        assertNull(SceneKeyAction.of(Key.D, isShiftPressed = false, isCommandPressed = false, step = 1f))
    }
}
