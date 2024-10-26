package com.pandulapeter.gameTemplate.engine.implementation.managers

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.pandulapeter.gameTemplate.engine.implementation.EngineImpl
import com.pandulapeter.gameTemplate.engine.implementation.helpers.consume
import com.pandulapeter.gameTemplate.engine.managers.InputManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class InputManagerImpl : InputManager {

    private var cache = mutableSetOf<Key>()
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

    fun emit() {
        if (cache.isNotEmpty()) {
            _activeKeys.tryEmit(cache.toSet())
        }
    }

    fun onKeyEvent(keyEvent: KeyEvent) = consume {
        if (keyEvent.type == KeyEventType.KeyDown) {
            if (!cache.contains(keyEvent.key)) {
                _onKeyPressed.tryEmit(keyEvent.key)
                cache.add(keyEvent.key)
            }
        }
        if (keyEvent.type == KeyEventType.KeyUp) {
            cache.remove(keyEvent.key)
            _onKeyReleased.tryEmit(keyEvent.key)
        }
    }
}