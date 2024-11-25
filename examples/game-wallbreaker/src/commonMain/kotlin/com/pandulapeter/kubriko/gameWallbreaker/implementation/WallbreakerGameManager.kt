package com.pandulapeter.kubriko.gameWallbreaker.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.gameWallbreaker.implementation.actors.Ball
import com.pandulapeter.kubriko.gameWallbreaker.implementation.actors.Brick
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.types.SceneOffset

internal class WallbreakerGameManager : Manager() {

    private lateinit var actorManager: ActorManager

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        val allBricks = (0..5).flatMap { y ->
            (-5..5).map { x ->
                Brick(
                    position = SceneOffset(
                        x = Brick.Width * x,
                        y = Brick.Height * y,
                    ),
                    hue = (0..360).random().toFloat(),
                )
            }
        }
        actorManager.add(actors = (allBricks + Ball()).toTypedArray())
    }
}