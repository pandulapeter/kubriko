/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.actors

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.ViewportManager
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyboardInputListenerTest {

    private var navigateBackCount = 0
    private var undoCount = 0
    private var redoCount = 0
    private var interactionModeSelectedCount = 0

    private fun newListener() = KeyboardInputListener(
        viewportManager = ViewportManager.newInstance(),
        keyboardInputManager = KeyboardInputManager.newInstance(),
        isTextInputFocused = { false },
        navigateBack = { navigateBackCount++ },
        onUndo = { undoCount++ },
        onRedo = { redoCount++ },
        onInteractionModeSelected = { interactionModeSelectedCount++ },
    )

    @Test
    fun escapeReleasedWithoutItsPressDoesNotNavigateBack() {
        newListener().onKeyReleased(Key.Escape)

        assertEquals(0, navigateBackCount)
    }

    @Test
    fun escapePressedAndReleasedNavigatesBackOnce() {
        val listener = newListener()

        listener.onKeyPressed(Key.Escape)
        listener.onKeyReleased(Key.Escape)
        listener.onKeyReleased(Key.Escape)

        assertEquals(1, navigateBackCount)
    }

    @Test
    fun backKeyBehavesLikeEscape() {
        val listener = newListener()

        listener.onKeyReleased(Key.Back)
        listener.onKeyPressed(Key.Back)
        listener.onKeyReleased(Key.Back)
        listener.onKeyReleased(Key.Back)

        assertEquals(1, navigateBackCount)
    }
}
