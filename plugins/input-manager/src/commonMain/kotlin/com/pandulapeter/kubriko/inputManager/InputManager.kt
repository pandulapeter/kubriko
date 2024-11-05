package com.pandulapeter.kubriko.inputManager

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.inputManager.implementation.InputManagerImpl
import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.flow.SharedFlow

/**
 * TODO: Documentation
 */
abstract class InputManager : Manager() {

    abstract val activeKeys: SharedFlow<Set<Key>>
    abstract val onKeyPressed: SharedFlow<Key>
    abstract val onKeyReleased: SharedFlow<Key>

    abstract fun isKeyPressed(key: Key): Boolean

    companion object {
        fun newInstance(): InputManager = InputManagerImpl()
    }
}