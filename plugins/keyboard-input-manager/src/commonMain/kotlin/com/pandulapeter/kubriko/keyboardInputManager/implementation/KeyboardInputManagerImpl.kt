package com.pandulapeter.kubriko.keyboardInputManager.implementation

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.implementation.helpers.eventFlow
import com.pandulapeter.kubriko.keyboardInputManager.KeyboardInputAware
import com.pandulapeter.kubriko.keyboardInputManager.KeyboardInputManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

internal class KeyboardInputManagerImpl : KeyboardInputManager() {

    private lateinit var actorManager: ActorManager
    private var activeKeysCache = mutableSetOf<Key>()
    private val _activeKeys = eventFlow<Set<Key>>()
    override val activeKeys = _activeKeys.asSharedFlow()
    private val _onKeyPressed = eventFlow<Key>()
    override val onKeyPressed = _onKeyPressed.asSharedFlow()
    private val _onKeyReleased = eventFlow<Key>()
    override val onKeyReleased = _onKeyReleased.asSharedFlow()
    private lateinit var keyboardEventHandler: KeyboardEventHandler
    private val keyboardInputAwareActors by lazy {
        actorManager.allActors
            .map { it.filterIsInstance<KeyboardInputAware>() }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    }

    override fun initialize(kubriko: Kubriko) {
        actorManager = kubriko.get<ActorManager>()
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
            activeKeysCache.toSet().let { activeKeys ->
                _activeKeys.tryEmit(activeKeys)
                keyboardInputAwareActors.value.forEach { it.handleActiveKeys(activeKeys) }
            }
        }
    }

    override fun onDispose() = keyboardEventHandler.stopListening()

    private fun onKeyPressed(key: Key) {
        if (!activeKeysCache.contains(key)) {
            _onKeyPressed.tryEmit(key)
            keyboardInputAwareActors.value.forEach { it.onKeyPressed(key) }
            activeKeysCache.add(key)
        }
    }

    private fun onKeyReleased(key: Key) {
        activeKeysCache.remove(key)
        keyboardInputAwareActors.value.forEach { it.onKeyReleased(key) }
        _onKeyReleased.tryEmit(key)
    }
}