package com.pandulapeter.kubriko.keyboardInput

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.actor.Actor
import kotlinx.collections.immutable.ImmutableSet

// TODO: Documentation
interface KeyboardInputAware : Actor {

    fun handleActiveKeys(activeKeys: ImmutableSet<Key>) = Unit

    fun onKeyPressed(key: Key) = Unit

    fun onKeyReleased(key: Key) = Unit
}