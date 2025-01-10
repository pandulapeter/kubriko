package com.pandulapeter.kubriko.keyboardInput

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.manager.Manager

/**
 * TODO: Documentation
 */
// TODO: Introduce a custom type instead of relying on Key
sealed class KeyboardInputManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "KeyboardInputManager",
) {

    abstract fun isKeyPressed(key: Key): Boolean

    companion object {
        fun newInstance(
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): KeyboardInputManager = KeyboardInputManagerImpl(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}