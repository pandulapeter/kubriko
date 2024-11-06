package com.pandulapeter.kubrikoKeyboardInputTest.implementation

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.keyboardInputManager.KeyboardInputAware
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class KeyboardInputTestManager : Manager(), KeyboardInputAware {

    private val _activeKeys = MutableStateFlow(emptySet<Key>())
    val activeKeys = _activeKeys.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        kubriko.require<ActorManager>().add(this)
    }

    override fun handleActiveKeys(activeKeys: Set<Key>) = _activeKeys.update { activeKeys }
}