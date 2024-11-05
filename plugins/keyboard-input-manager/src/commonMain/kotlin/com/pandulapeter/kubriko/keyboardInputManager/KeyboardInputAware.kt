package com.pandulapeter.kubriko.keyboardInputManager

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.actor.Actor

// TODO: Documentation
interface KeyboardInputAware : Actor {

    fun handleActiveKeys(activeKeys: Set<Key>) = Unit

    fun onKeyPressed(key: Key) = Unit

    fun onKeyReleased(key: Key) = Unit
}