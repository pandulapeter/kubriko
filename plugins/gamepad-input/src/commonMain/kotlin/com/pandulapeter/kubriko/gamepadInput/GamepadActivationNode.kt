/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gamepadInput

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState

internal class GamepadActivationNode(
    private var gamepadInputManager: GamepadInputManager,
    var onActivation: () -> Unit,
) : Modifier.Node(), FocusEventModifierNode {

    private var isFocused = false

    override fun onFocusEvent(focusState: FocusState) = setFocused(focusState.isFocused)

    override fun onDetach() = setFocused(false)

    // A focused node that is handed a different manager carries its claim across, or the manager it is leaving
    // would be left activating a control that is no longer listening to it.
    fun setGamepadInputManager(gamepadInputManager: GamepadInputManager) {
        if (this.gamepadInputManager === gamepadInputManager) {
            return
        }
        val wasFocused = isFocused
        setFocused(false)
        this.gamepadInputManager = gamepadInputManager
        setFocused(wasFocused)
    }

    fun activate() = onActivation()

    private fun setFocused(isFocused: Boolean) {
        if (this.isFocused == isFocused) {
            return
        }
        this.isFocused = isFocused
        if (isFocused) {
            gamepadInputManager.onActivationTargetFocused(this)
        } else {
            gamepadInputManager.onActivationTargetUnfocused(this)
        }
    }
}
