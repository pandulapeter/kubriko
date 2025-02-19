/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.actors

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.handleKeyPressed
import com.pandulapeter.kubriko.sceneEditor.implementation.helpers.handleKeys
import kotlinx.collections.immutable.ImmutableSet

internal class KeyboardInputListener(
    private val viewportManager: ViewportManager,
    private val navigateBack: () -> Unit,
) : KeyboardInputAware, Unique {

    override fun handleActiveKeys(activeKeys: ImmutableSet<Key>) = viewportManager.handleKeys(activeKeys)

    override fun onKeyReleased(key: Key) = handleKeyPressed(
        key = key,
        onNavigateBackRequested = navigateBack,
    )
}