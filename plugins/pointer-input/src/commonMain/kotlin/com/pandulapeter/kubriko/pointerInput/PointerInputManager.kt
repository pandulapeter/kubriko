package com.pandulapeter.kubriko.pointerInput

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.flow.StateFlow

/**
 * TODO: Documentation
 */
sealed class PointerInputManager : Manager() {

    abstract val isPointerPressed: StateFlow<Boolean>
    abstract val pointerScreenOffset: StateFlow<Offset?>

    companion object {
        fun newInstance(
            isActiveAboveViewport: Boolean = false,
        ): PointerInputManager = PointerInputManagerImpl(
            isActiveAboveViewport = isActiveAboveViewport,
        )
    }
}