package com.pandulapeter.kubriko.gameWallbreaker.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.gameWallbreaker.implementation.actors.FogShader
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager

internal class WallbreakerBackgroundManager : Manager() {

    override fun onInitialize(kubriko: Kubriko) = kubriko.get<ActorManager>().add(FogShader())
}