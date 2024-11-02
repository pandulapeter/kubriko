package com.pandulapeter.kubriko.implementation.managers

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.implementation.KubrikoImpl
import com.pandulapeter.kubriko.implementation.helpers.eventFlow
import com.pandulapeter.kubriko.managers.InputManager
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class InputManagerImpl(engineImpl: KubrikoImpl) : InputManager {

    private var activeKeysCache = mutableSetOf<Key>()
    private val _activeKeys = eventFlow<Set<Key>>()
    override val activeKeys = _activeKeys.asSharedFlow()
    private val _onKeyPressed = eventFlow<Key>()
    override val onKeyPressed = _onKeyPressed.asSharedFlow()
    private val _onKeyReleased = eventFlow<Key>()
    override val onKeyReleased = _onKeyReleased.asSharedFlow()

    init {
        engineImpl.stateManager.isFocused
            .filterNot { it }
            .onEach {
                activeKeysCache.forEach(_onKeyReleased::tryEmit)
                activeKeysCache.clear()
            }
            .launchIn(engineImpl)
    }

    override fun isKeyPressed(key: Key) = activeKeysCache.contains(key)

    fun emit() {
        if (activeKeysCache.isNotEmpty()) {
            _activeKeys.tryEmit(activeKeysCache.toSet())
        }
    }

    fun onKeyPressed(key: Key) {
        if (!activeKeysCache.contains(key)) {
            _onKeyPressed.tryEmit(key)
            activeKeysCache.add(key)
        }
    }

    fun onKeyReleased(key: Key) {
        activeKeysCache.remove(key)
        _onKeyReleased.tryEmit(key)
    }
}