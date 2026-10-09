/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
@file:JvmName("GamepadFocusNavigationKt")
@file:JvmMultifileClass

package com.pandulapeter.kubriko.gamepadInput

import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

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
