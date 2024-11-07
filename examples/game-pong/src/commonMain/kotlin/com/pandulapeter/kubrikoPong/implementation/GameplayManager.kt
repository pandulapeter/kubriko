package com.pandulapeter.kubrikoPong.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubrikoPong.implementation.actors.Ball

internal class GameplayManager : Manager() {

    override fun onInitialize(kubriko: Kubriko) = kubriko.require<ActorManager>().add(
        Ball()
    )
}