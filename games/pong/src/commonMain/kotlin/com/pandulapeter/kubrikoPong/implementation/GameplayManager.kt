package com.pandulapeter.kubrikoPong.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubrikoPong.implementation.actors.Ball
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// TODO: Implement Pong game logic
internal class GameplayManager : Manager() {

    private lateinit var actorManager: ActorManager
    lateinit var stateManager: StateManager
        private set

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        stateManager = kubriko.require()
        stateManager.isFocused
            .filterNot { it }
            .onEach { stateManager.updateIsRunning(false) }
            .launchIn(scope)
        actorManager.add(Ball())
    }
}