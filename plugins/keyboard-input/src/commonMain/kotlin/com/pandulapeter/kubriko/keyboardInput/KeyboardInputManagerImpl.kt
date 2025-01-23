/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.keyboardInput

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.keyboardInput.implementation.KeyboardEventHandler
import com.pandulapeter.kubriko.keyboardInput.implementation.createKeyboardEventHandler
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal class KeyboardInputManagerImpl(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : KeyboardInputManager(isLoggingEnabled, instanceNameForLogging) {
    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()
    private var activeKeysCache = mutableSetOf<Key>()
    private var keyboardEventHandler: KeyboardEventHandler? = null
    private val keyboardInputAwareActors by autoInitializingLazy {
        actorManager.allActors.map { it.filterIsInstance<KeyboardInputAware>() }.asStateFlow(emptyList())
    }
    private var hasSentEmptyMap = false

    override fun onInitialize(kubriko: Kubriko) {
        stateManager.isFocused
            .filterNot { it }
            .onEach { activeKeysCache.forEach(::onKeyReleased) }
            .launchIn(scope)
    }

    @Composable
    override fun Composable(insetPaddingModifier: Modifier) {
        if (keyboardEventHandler?.isValid() == false) {
            keyboardEventHandler?.stopListening()
            keyboardEventHandler = null
        }
        if (keyboardEventHandler == null && isInitialized.value) {
            keyboardEventHandler = createKeyboardEventHandler(
                onKeyPressed = ::onKeyPressed,
                onKeyReleased = ::onKeyReleased,
                coroutineScope = scope,
            ).also { it.startListening() }
        }
    }

    override fun isKeyPressed(key: Key) = activeKeysCache.contains(key)

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        if (activeKeysCache.isNotEmpty() && stateManager.isFocused.value) {
            hasSentEmptyMap = false
            activeKeysCache.toImmutableSet().let { activeKeys ->
                keyboardInputAwareActors.value.forEach { it.handleActiveKeys(activeKeys) }
            }
        } else {
            if (!hasSentEmptyMap) {
                hasSentEmptyMap = true
                activeKeysCache.toImmutableSet().let { activeKeys ->
                    keyboardInputAwareActors.value.forEach { it.handleActiveKeys(activeKeys) }
                }
            }
        }
    }

    override fun onDispose() {
        keyboardEventHandler?.stopListening()
        keyboardEventHandler = null
    }

    private fun onKeyPressed(key: Key) {
        if (!activeKeysCache.contains(key) && stateManager.isFocused.value) {
            keyboardInputAwareActors.value.forEach { it.onKeyPressed(key) }
            activeKeysCache.add(key)
        }
    }

    private fun onKeyReleased(key: Key) {
        keyboardInputAwareActors.value.forEach { it.onKeyReleased(key) }
        activeKeysCache.remove(key)
    }
}