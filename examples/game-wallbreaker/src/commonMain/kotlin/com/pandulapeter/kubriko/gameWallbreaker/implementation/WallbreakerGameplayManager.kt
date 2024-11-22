package com.pandulapeter.kubriko.gameWallbreaker.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.gameWallbreaker.implementation.actors.Ball
import com.pandulapeter.kubriko.gameWallbreaker.implementation.actors.Brick
import kotlinx.coroutines.flow.map

internal class WallbreakerGameplayManager : Manager() {

    private lateinit var actorManager: ActorManager
    private lateinit var metadataManager: MetadataManager
    private val bricks by autoInitializingLazy {
        actorManager.allActors.map { it.filterIsInstance<Brick>() }.asStateFlow(emptyList())
    }

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        metadataManager = kubriko.require()
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
        actorManager.add(actors = (allBricks + Ball(bricks = bricks)).toTypedArray())
    }
}