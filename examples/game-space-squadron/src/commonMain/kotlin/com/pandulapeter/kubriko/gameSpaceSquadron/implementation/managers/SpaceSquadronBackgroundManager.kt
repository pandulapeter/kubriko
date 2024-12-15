package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.SpaceBackgroundShader
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager

internal class SpaceSquadronBackgroundManager : Manager() {

    override fun onInitialize(kubriko: Kubriko) = kubriko.get<ActorManager>().add(SpaceBackgroundShader())
}