package com.pandulapeter.kubriko.keyboardInput.implementation

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal class KeyboardInputManagerImpl : KeyboardInputManager() {

    private lateinit var actorManager: ActorManager
    private lateinit var stateManager: StateManager
    private var activeKeysCache = mutableSetOf<Key>()
    private lateinit var keyboardEventHandler: KeyboardEventHandler
    private val keyboardInputAwareActors by autoInitializingLazy {
        actorManager.allActors.map { it.filterIsInstance<KeyboardInputAware>() }.asStateFlow(emptyList())
    }
    private var hasSentEmptyMap = false

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require<ActorManager>()
        stateManager = kubriko.require<StateManager>()
        stateManager.isFocused
            .filterNot { it }
            .onEach { activeKeysCache.forEach(::onKeyReleased) }
            .launchIn(scope)
    }

    override fun isKeyPressed(key: Key) = activeKeysCache.contains(key)

    @Composable
    override fun onRecomposition() {
        keyboardEventHandler = rememberKeyboardEventHandler(
            onKeyPressed = ::onKeyPressed,
            onKeyReleased = ::onKeyReleased,
        )
    }

    override fun onLaunch() = keyboardEventHandler.startListening()

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) {
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