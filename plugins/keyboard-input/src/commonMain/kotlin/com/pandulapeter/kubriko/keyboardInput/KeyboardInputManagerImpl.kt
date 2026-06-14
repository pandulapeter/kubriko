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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.keyboardInput.implementation.KeyboardEventHandler
import com.pandulapeter.kubriko.keyboardInput.implementation.createKeyboardEventHandler
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
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
    // Keys pressed since the previous tick. Discrete onKeyPressed/onKeyReleased callbacks fire off-tick
    // and are never lost, but a key tapped and released entirely between two ticks (common at low frame
    // rates) would otherwise never appear in the per-tick handleActiveKeys snapshot. This latch surfaces
    // such a key for exactly one tick. Cleared at the end of every onUpdate.
    private val keysPressedSinceLastSnapshot = mutableSetOf<Key>()
    private var activeKeysSnapshot: ImmutableSet<Key> = persistentSetOf()
    private var isActiveKeysDirty = false
    private var keyboardEventHandler: KeyboardEventHandler? = null
    private val keyboardInputAwareActors by autoInitializingLazy {
        actorManager.allActors.map { it.filterIsInstance<KeyboardInputAware>() }.asStateFlowOnMainThread(emptyList())
    }
    private var hasSentEmptyMap = false

    override fun onInitialize(kubriko: Kubriko) {
        stateManager.isFocused
            .filterNot { it }
            .onEach { activeKeysCache.forEach(::onKeyReleased) }
            .launchIn(scope)
    }

    @Composable
    override fun Composable(windowInsets: WindowInsets) {
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
        val hasLatchedKeys = keysPressedSinceLastSnapshot.isNotEmpty()
        if (isActiveKeysDirty) {
            activeKeysSnapshot = buildActiveKeysSnapshot()
            // When the snapshot exists only to surface latch-only keys (already released), force a rebuild
            // next tick so they are dropped once they have been observed for one tick; otherwise the dirty
            // flag clears as before.
            isActiveKeysDirty = hasLatchedKeys && !activeKeysCache.containsAll(keysPressedSinceLastSnapshot)
        }
        if ((activeKeysCache.isNotEmpty() || hasLatchedKeys) && stateManager.isFocused.value) {
            hasSentEmptyMap = false
            keyboardInputAwareActors.value.forEach { it.handleActiveKeys(activeKeysSnapshot) }
        } else {
            if (!hasSentEmptyMap) {
                hasSentEmptyMap = true
                keyboardInputAwareActors.value.forEach { it.handleActiveKeys(activeKeysSnapshot) }
            }
        }
        keysPressedSinceLastSnapshot.clear()
    }

    private fun buildActiveKeysSnapshot(): ImmutableSet<Key> {
        if (keysPressedSinceLastSnapshot.isEmpty() || activeKeysCache.containsAll(keysPressedSinceLastSnapshot)) {
            return activeKeysCache.toImmutableSet()
        }
        // A key was tapped (pressed and released) between two ticks; surface it alongside the held keys
        // so per-tick polling via handleActiveKeys observes it once. Allocates only on this rare path.
        val union = HashSet<Key>(activeKeysCache.size + keysPressedSinceLastSnapshot.size)
        union.addAll(activeKeysCache)
        union.addAll(keysPressedSinceLastSnapshot)
        return union.toImmutableSet()
    }

    override fun onDispose() {
        keyboardEventHandler?.stopListening()
        keyboardEventHandler = null
    }

    private fun onKeyPressed(key: Key) {
        if (!activeKeysCache.contains(key) && stateManager.isFocused.value) {
            keyboardInputAwareActors.value.forEach { it.onKeyPressed(key) }
            activeKeysCache.add(key)
            keysPressedSinceLastSnapshot.add(key)
            isActiveKeysDirty = true
        }
    }

    private fun onKeyReleased(key: Key) {
        keyboardInputAwareActors.value.forEach { it.onKeyReleased(key) }
        activeKeysCache.remove(key)
        isActiveKeysDirty = true
    }
}