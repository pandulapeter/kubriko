/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.keyboardInput.implementation

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import kotlinx.coroutines.CoroutineScope

internal interface KeyboardEventHandler {

    @Composable
    fun isValid(): Boolean

    fun startListening()

    fun stopListening()
}

@Composable
internal expect fun createKeyboardEventHandler(
    onKeyPressed: (Key) -> Unit,
    onKeyReleased: (Key) -> Unit,
    coroutineScope: CoroutineScope,
): KeyboardEventHandler