package com.pandulapeter.kubriko.keyboardInputManager

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.keyboardInputManager.implementation.KeyboardInputManagerImpl
import com.pandulapeter.kubriko.manager.Manager

/**
 * TODO: Documentation
 */
abstract class KeyboardInputManager : Manager() {

    abstract fun isKeyPressed(key: Key): Boolean

    companion object {
        fun newInstance(): KeyboardInputManager = KeyboardInputManagerImpl()
    }
}