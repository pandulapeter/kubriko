package com.pandulapeter.kubriko.keyboardInputManager.implementation

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.keyboardInputManager.KeyboardInputAware
import com.pandulapeter.kubriko.keyboardInputManager.KeyboardInputManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

internal class KeyboardInputManagerImpl : KeyboardInputManager() {

    private lateinit var actorManager: ActorManager
    private lateinit var stateManager: StateManager
    private var activeKeysCache = mutableSetOf<Key>()
    private lateinit var keyboardEventHandler: KeyboardEventHandler
    private val keyboardInputAwareActors by lazy {
        actorManager.allActors
            .map { it.filterIsInstance<KeyboardInputAware>() }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    }

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require<ActorManager>()
        stateManager = kubriko.require<StateManager>()
        keyboardInputAwareActors.value // Make sure the listeners are initialized TODO: HACK
        stateManager.isFocused
            .filterNot { it }
            .onEach { activeKeysCache.forEach(::onKeyReleased) }
            .launchIn(scope)
    }

    override fun isKeyPressed(key: Key) = activeKeysCache.contains(key)

    @Composable
    override fun onCreateModifier() = Modifier.focusable()

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
            activeKeysCache.toSet().let { activeKeys ->
                keyboardInputAwareActors.value.forEach { it.handleActiveKeys(activeKeys) }
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