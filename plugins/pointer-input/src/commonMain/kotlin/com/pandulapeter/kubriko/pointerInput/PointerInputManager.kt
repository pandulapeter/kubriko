package com.pandulapeter.kubriko.pointerInput

import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.pointerInput.implementation.PointerInputManagerImpl

/**
 * TODO: Documentation
 */
abstract class PointerInputManager : Manager() {

    companion object {
        fun newInstance(): PointerInputManager = PointerInputManagerImpl()
    }
}