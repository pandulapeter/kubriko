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
import com.pandulapeter.kubriko.actor.Actor
import kotlinx.collections.immutable.ImmutableSet

// TODO: Documentation
interface KeyboardInputAware : Actor {

    fun handleActiveKeys(activeKeys: ImmutableSet<Key>) = Unit

    fun onKeyPressed(key: Key) = Unit

    fun onKeyReleased(key: Key) = Unit
}