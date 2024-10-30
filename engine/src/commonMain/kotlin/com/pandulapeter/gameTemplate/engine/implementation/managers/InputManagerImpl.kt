package com.pandulapeter.gameTemplate.engine.implementation.managers

import androidx.compose.ui.input.key.Key
import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import com.pandulapeter.gameTemplate.engine.managers.InputManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class InputManagerImpl : InputManager {

    private var activeKeysCache = mutableSetOf<Key>()
    private val _activeKeys = MutableSharedFlow<Set<Key>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val activeKeys = _activeKeys.asSharedFlow()
    private val _onKeyPressed = MutableSharedFlow<Key>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val onKeyPressed = _onKeyPressed.asSharedFlow()
    private val _onKeyReleased = MutableSharedFlow<Key>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val onKeyReleased = _onKeyReleased.asSharedFlow()

    init {
        EngineImpl.stateManager.isFocused
            .filterNot { it }
            .onEach { _activeKeys.emit(emptySet()) }
            .launchIn(EngineImpl)
    }

    override fun isKeyPressed(key: Key) = activeKeysCache.contains(key)

    fun emit() {
        if (activeKeysCache.isNotEmpty()) {
            _activeKeys.tryEmit(activeKeysCache.toSet())
        }
    }

    override fun onKeyPressed(key: Key) {
        if (!activeKeysCache.contains(key)) {
            _onKeyPressed.tryEmit(key)
            activeKeysCache.add(key)
        }
    }

    override fun onKeyReleased(key: Key) {
        activeKeysCache.remove(key)
        _onKeyReleased.tryEmit(key)
    }
}