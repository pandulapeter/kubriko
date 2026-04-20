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

/**
 * A trait that can be added to an [Actor] to make it aware of keyboard input.
 */
interface KeyboardInputAware : Actor {

    /**
     * Called on every frame with the set of all keys currently being held down.
     * TODO: On Android continuously pressed keys are not reported correctly.
     *
     * @param activeKeys The set of all keys currently pressed.
     */
    fun handleActiveKeys(activeKeys: ImmutableSet<Key>) = Unit

    /**
     * Called when a key is first pressed.
     *
     * @param key The key that was pressed.
     */
    fun onKeyPressed(key: Key) = Unit

    /**
     * Called when a key is released.
     *
     * @param key The key that was released.
     */
    fun onKeyReleased(key: Key) = Unit
}