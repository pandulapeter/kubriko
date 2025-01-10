package com.pandulapeter.kubriko.pointerInput

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
sealed class PointerInputManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "PointerInputManager",
) {
    abstract val isPointerPressed: StateFlow<Boolean>
    abstract val pointerScreenOffset: StateFlow<Offset?>

    // TODO: Only works on Desktop. On macOS it requires accessibility permission
    abstract fun movePointer(offset: Offset)

    companion object {
        fun newInstance(
            isActiveAboveViewport: Boolean = false,
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): PointerInputManager = PointerInputManagerImpl(
            isActiveAboveViewport = isActiveAboveViewport,
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}