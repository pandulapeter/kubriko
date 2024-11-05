package com.pandulapeter.kubriko.inputManager.implementation

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.implementation.helpers.eventFlow
import com.pandulapeter.kubriko.inputManager.InputManager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class InputManagerImpl : InputManager() {

    private var activeKeysCache = mutableSetOf<Key>()
    private val _activeKeys = eventFlow<Set<Key>>()
    override val activeKeys = _activeKeys.asSharedFlow()
    private val _onKeyPressed = eventFlow<Key>()
    override val onKeyPressed = _onKeyPressed.asSharedFlow()
    private val _onKeyReleased = eventFlow<Key>()
    override val onKeyReleased = _onKeyReleased.asSharedFlow()
    private lateinit var keyboardEventHandler: KeyboardEventHandler

    override fun initialize(kubriko: Kubriko) {
        kubriko.get<StateManager>().isFocused
            .filterNot { it }
            .onEach {
                activeKeysCache.forEach(_onKeyReleased::tryEmit)
                activeKeysCache.clear()
            }
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
        if (activeKeysCache.isNotEmpty()) {
            _activeKeys.tryEmit(activeKeysCache.toSet())
        }
    }

    override fun onDispose() = keyboardEventHandler.stopListening()

    private fun onKeyPressed(key: Key) {
        if (!activeKeysCache.contains(key)) {
            _onKeyPressed.tryEmit(key)
            activeKeysCache.add(key)
        }
    }

    private fun onKeyReleased(key: Key) {
        activeKeysCache.remove(key)
        _onKeyReleased.tryEmit(key)
    }
}