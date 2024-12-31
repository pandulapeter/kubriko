package com.pandulapeter.kubriko.keyboardInput

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.keyboardInput.implementation.createKeyboardEventHandler
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal class KeyboardInputManagerImpl : KeyboardInputManager() {

    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()
    private var activeKeysCache = mutableSetOf<Key>()
    private val keyboardEventHandler by autoInitializingLazy {
        createKeyboardEventHandler(
            onKeyPressed = ::onKeyPressed,
            onKeyReleased = ::onKeyReleased,
            coroutineScope = scope,
        )
    }
    private val keyboardInputAwareActors by autoInitializingLazy {
        actorManager.allActors.map { it.filterIsInstance<KeyboardInputAware>() }.asStateFlow(emptyList())
    }
    private var hasSentEmptyMap = false

    override fun onInitialize(kubriko: Kubriko) {
        stateManager.isFocused
            .filterNot { it }
            .onEach { activeKeysCache.forEach(::onKeyReleased) }
            .launchIn(scope)
        keyboardEventHandler.startListening()
    }

    override fun isKeyPressed(key: Key) = activeKeysCache.contains(key)

    override fun onUpdate(deltaTimeInMilliseconds: Float, gameTimeMilliseconds: Long) {
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

    override fun onDispose() = keyboardEventHandler.stopListening()

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