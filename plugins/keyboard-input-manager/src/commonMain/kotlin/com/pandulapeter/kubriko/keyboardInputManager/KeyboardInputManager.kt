package com.pandulapeter.kubriko.keyboardInputManager

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.keyboardInputManager.implementation.KeyboardInputManagerImpl
import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.flow.SharedFlow

/**
 * TODO: Documentation
 */
abstract class KeyboardInputManager : Manager() {

    // TODO: All of these properties should be removed
    abstract val activeKeys: SharedFlow<Set<Key>>
    abstract val onKeyPressed: SharedFlow<Key>
    abstract val onKeyReleased: SharedFlow<Key>

    abstract fun isKeyPressed(key: Key): Boolean

    companion object {
        fun newInstance(): KeyboardInputManager = KeyboardInputManagerImpl()
    }
}