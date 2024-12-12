package com.pandulapeter.kubriko.gameWallbreaker.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.AudioPlaybackManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.actors.Ball
import com.pandulapeter.kubriko.gameWallbreaker.implementation.actors.Brick
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.shader.collection.ChromaticAberrationShader
import com.pandulapeter.kubriko.shader.collection.SmoothPixelationShader
import com.pandulapeter.kubriko.shader.collection.VignetteShader
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kubriko.examples.game_wallbreaker.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

internal class WallbreakerGameManager : Manager() {

    private lateinit var actorManager: ActorManager
    private lateinit var audioPlaybackManager: AudioPlaybackManager
    private lateinit var metadataManager: MetadataManager
    private lateinit var stateManager: StateManager

    @ExperimentalResourceApi
    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        audioPlaybackManager = kubriko.require()
        metadataManager = kubriko.require()
        stateManager = kubriko.require()
        val allBricks = (-8..1).flatMap { y ->
            (-4..4).map { x ->
                Brick(
                    position = SceneOffset(
                        x = Brick.Width * x,
                        y = Brick.Height * y,
                    ),
                    hue = (0..360).random().toFloat(),
                )
            }
        }
        actorManager.add(allBricks + Ball() + SmoothPixelationShader() + VignetteShader() + ChromaticAberrationShader())
        stateManager.isFocused
            .filterNot { it }
            .onEach { stateManager.updateIsRunning(false) }
            .launchIn(scope)
        audioPlaybackManager.preloadSound(
            Res.getUri("files/sounds/click.wav"),
            Res.getUri("files/sounds/pop.wav"),
        )
        audioPlaybackManager.playMusic(
            uri = Res.getUri("files/music/music.mp3"),
            shouldLoop = true,
        )
    }
}