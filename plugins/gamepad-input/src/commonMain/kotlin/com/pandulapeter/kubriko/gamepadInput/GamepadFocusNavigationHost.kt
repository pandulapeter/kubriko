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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import com.pandulapeter.kubriko.KubrikoViewport
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

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
