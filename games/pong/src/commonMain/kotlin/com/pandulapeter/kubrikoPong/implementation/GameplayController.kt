package com.pandulapeter.kubrikoPong.implementation

import androidx.compose.ui.input.key.Key
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.InputManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubrikoPong.implementation.actors.Ball
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
    private val actorManager = kubriko.get<ActorManager>()
    private val inputManager = kubriko.get<InputManager>()
    val stateManager = kubriko.get<StateManager>()

    init {
        stateManager.isFocused
            .filterNot { it }
            .onEach { stateManager.updateIsRunning(false) }
            .launchIn(this)
        inputManager.onKeyReleased
            .onEach(::handleKeyReleased)
            .launchIn(this)
        actorManager.add(Ball())
    }

    private fun handleKeyReleased(key: Key) {
        when (key) {
            Key.Escape, Key.Back, Key.Backspace -> stateManager.updateIsRunning(!stateManager.isRunning.value)
        }
    }
}