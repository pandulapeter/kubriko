package com.pandulapeter.kubriko.pointerInput

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
sealed class PointerInputManager(isLoggingEnabled: Boolean) : Manager(isLoggingEnabled) {

    abstract val isPointerPressed: StateFlow<Boolean>
    abstract val pointerScreenOffset: StateFlow<Offset?>

    // TODO: Only works on Desktop. On macOS it requires accessibility permission
    abstract fun movePointer(offset: Offset)

    companion object {
        fun newInstance(
            isActiveAboveViewport: Boolean = false,
            isLoggingEnabled: Boolean = false,
        ): PointerInputManager = PointerInputManagerImpl(
            isActiveAboveViewport = isActiveAboveViewport,
            isLoggingEnabled = isLoggingEnabled,
        )
    }
}