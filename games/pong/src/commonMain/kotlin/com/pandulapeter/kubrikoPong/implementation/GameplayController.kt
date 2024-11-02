package com.pandulapeter.kubrikoPong.implementation

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubrikoPong.implementation.gameObjects.Ball
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// TODO: Implement Pong game logic
internal class GameplayController(
    val kubriko: Kubriko,
) : CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

    init {
        kubriko.stateManager.isFocused
            .filterNot { it }
            .onEach { kubriko.stateManager.updateIsRunning(false) }
            .launchIn(this)
        kubriko.inputManager.onKeyReleased
            .onEach(::handleKeyReleased)
            .launchIn(this)
        kubriko.actorManager.add(Ball())
    }

    private fun handleKeyReleased(key: Key) {
        when (key) {
            Key.Escape, Key.Back, Key.Backspace -> kubriko.stateManager.updateIsRunning(!kubriko.stateManager.isRunning.value)
        }
    }
}