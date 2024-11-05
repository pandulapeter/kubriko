package com.pandulapeter.kubrikoStressTest.implementation.actors

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.keyboardInputManager.KeyboardInputAware
import com.pandulapeter.kubriko.keyboardInputManager.extensions.KeyboardZoomState
import com.pandulapeter.kubriko.keyboardInputManager.extensions.zoomState
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubrikoStressTest.implementation.GameplayController

internal class KeyboardInputListener(private val stateManager: StateManager) : KeyboardInputAware, Unique {

    override fun handleActiveKeys(activeKeys: Set<Key>) {
        if (stateManager.isRunning.value) {
            GameplayController.viewportManager.multiplyScaleFactor(
                when (activeKeys.zoomState) {
                    KeyboardZoomState.NONE -> 1f
                    KeyboardZoomState.ZOOM_IN -> 1.02f
                    KeyboardZoomState.ZOOM_OUT -> 0.98f
                }
            )
        }
    }

    override fun onKeyReleased(key: Key) = when (key) {
        Key.Escape, Key.Back, Key.Backspace -> stateManager.updateIsRunning(!stateManager.isRunning.value)
        else -> Unit
    }
}