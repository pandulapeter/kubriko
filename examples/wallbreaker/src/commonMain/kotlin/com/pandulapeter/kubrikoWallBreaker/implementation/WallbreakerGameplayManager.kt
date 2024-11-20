package com.pandulapeter.kubrikoWallbreaker.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubrikoWallbreaker.implementation.actors.Ball
import com.pandulapeter.kubrikoWallbreaker.implementation.actors.Brick

internal class WallbreakerGameplayManager : Manager() {

    private lateinit var actorManager: ActorManager
    private lateinit var metadataManager: MetadataManager

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        metadataManager = kubriko.require()
        val bricks = (0..5).flatMap { y ->
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
        actorManager.add(actors = (bricks + Ball()).toTypedArray())
    }
}