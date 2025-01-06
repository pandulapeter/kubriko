package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.Ship
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.ShipDestination
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager

internal class SpaceSquadronGameManager : Manager() {

    private val actorManager by manager<ActorManager>()
    private val stateManager by manager<StateManager>()

    override fun onInitialize(kubriko: Kubriko) = kubriko.get<ActorManager>().add(Ship())

    fun playGame() {
        actorManager.allActors.value.filterIsInstance<ShipDestination>().firstOrNull()?.resetPointerTracking()
        stateManager.updateIsRunning(true)
    }

    fun pauseGame() = stateManager.updateIsRunning(false)
}