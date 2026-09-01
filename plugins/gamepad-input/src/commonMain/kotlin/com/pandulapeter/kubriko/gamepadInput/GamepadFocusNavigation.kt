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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import com.pandulapeter.kubriko.KubrikoViewport

/**
 * Points [GamepadInputManager.isFocusNavigationEnabled] at the composition this is placed in, for as long as it
 * is on screen: the focus its sticks walk, and what [GamepadButton.EAST] backs out of.
 *
 * Compose gives content shown in a `Popup` or a `Dialog` a focus system of its own, separate from the window
 * behind it, and the sticks can only ever drive one of them. Placing this inside such content is what hands them
 * over to it: the innermost host on screen is the one they drive, and the one behind it takes them back when it
 * is dismissed.
 *
 * ```
 * DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
 *     GamepadFocusNavigationHost(gamepadInputManager, onBack = { isExpanded = false })
 *     // ...
 * }
 * ```
 *
 * The [KubrikoViewport] the manager belongs to already hosts the window's own, so a game only ever places one
 * for content Compose has moved out of the window, or to claim [onBack] for a screen of its own.
 *
 * @param onBack What the east face button does while this is the innermost host - dismissing the popup, closing
 * the menu, whatever backing out means here. A host that leaves it null leaves the button doing nothing rather
 * than passing it to the host behind it, which would back out of two things at once.
 */
@Composable
fun GamepadFocusNavigationHost(
    gamepadInputManager: GamepadInputManager,
    onBack: (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val inputModeManager = LocalInputModeManager.current
    val currentOnBack by rememberUpdatedState(onBack)
    DisposableEffect(gamepadInputManager, focusManager, inputModeManager) {
        val host = GamepadFocusNavigationHostState(
            focusManager = focusManager,
            inputModeManager = inputModeManager,
            onBack = { currentOnBack?.invoke() },
            hasOnBack = { currentOnBack != null },
        )
        gamepadInputManager.onFocusNavigationHostAttached(host)
        onDispose { gamepadInputManager.onFocusNavigationHostDetached(host) }
    }
}

internal class GamepadFocusNavigationHostState(
    val focusManager: FocusManager,
    val inputModeManager: InputModeManager,
    val onBack: () -> Unit,
    val hasOnBack: () -> Boolean,
)

/**
 * What [GamepadButton.SOUTH] does while this Composable holds the focus, for as long as
 * [GamepadInputManager.isFocusNavigationEnabled] is on.
 *
 * Pass the same action the control runs when it is clicked; this only adds the gamepad to the ways of reaching
 * it, and changes nothing about how the control behaves for a pointer or a keyboard. Compose already activates
 * a focused `clickable` from Enter, Space and the D-pad center of a real keyboard, and it stays in charge of
 * that - a gamepad is simply not a keyboard on any platform this engine runs on, and only Android turns one
 * into those keys, so this is what gives every platform the same behavior.
 *
 * ```
 * Modifier
 *     .clickable(onClick = ::toggle)
 *     .onGamepadActivation(gamepadInputManager, onActivation = ::toggle)
 * ```
 *
 * Only one Composable can be focused at a time, so only one action is ever live. A control that loses the focus
 * or leaves the composition takes its action with it.
 */
fun Modifier.onGamepadActivation(
    gamepadInputManager: GamepadInputManager,
    onActivation: () -> Unit,
): Modifier = this then GamepadActivationElement(
    gamepadInputManager = gamepadInputManager,
    onActivation = onActivation,
)

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

private data class GamepadActivationElement(
    private val gamepadInputManager: GamepadInputManager,
    private val onActivation: () -> Unit,
) : ModifierNodeElement<GamepadActivationNode>() {

    override fun create() = GamepadActivationNode(
        gamepadInputManager = gamepadInputManager,
        onActivation = onActivation,
    )

    override fun update(node: GamepadActivationNode) {
        node.onActivation = onActivation
        node.setGamepadInputManager(gamepadInputManager)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "onGamepadActivation"
        properties["gamepadInputManager"] = gamepadInputManager
        properties["onActivation"] = onActivation
    }
}
