package com.pandulapeter.kubriko.manager

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.implementation.manager.InputManagerImpl
import kotlinx.coroutines.flow.SharedFlow

/**
 * TODO: Documentation
 */
// TODO: Move to plugin
abstract class InputManager : Manager() {

    abstract val activeKeys: SharedFlow<Set<Key>>
    abstract val onKeyPressed: SharedFlow<Key>
    abstract val onKeyReleased: SharedFlow<Key>

    abstract fun isKeyPressed(key: Key): Boolean

    companion object {
        fun newInstance(): InputManager = InputManagerImpl()
    }
}