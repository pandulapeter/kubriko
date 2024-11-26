package com.pandulapeter.kubriko.demoPerformance.implementation.actors

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.keyboardInput.extensions.KeyboardZoomState
import com.pandulapeter.kubriko.keyboardInput.extensions.zoomState
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import kotlinx.collections.immutable.ImmutableSet

internal class KeyboardInputListener : KeyboardInputAware, Unique {

    private lateinit var stateManager: StateManager
    private lateinit var viewportManager: ViewportManager

    override fun onAdded(kubriko: Kubriko) {
        stateManager = kubriko.require()
        viewportManager = kubriko.require()
    }

    override fun handleActiveKeys(activeKeys: ImmutableSet<Key>) {
        if (stateManager.isRunning.value) {
            viewportManager.multiplyScaleFactor(
                when (activeKeys.zoomState) {
                    KeyboardZoomState.NONE -> 1f
                    KeyboardZoomState.ZOOM_IN -> 1.02f
                    KeyboardZoomState.ZOOM_OUT -> 0.98f
                }
            )
        }
    }
}