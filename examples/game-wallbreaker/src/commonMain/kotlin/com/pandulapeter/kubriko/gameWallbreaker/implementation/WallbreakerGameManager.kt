package com.pandulapeter.kubriko.gameWallbreaker.implementation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Group
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.gameWallbreaker.implementation.actors.Ball
import com.pandulapeter.kubriko.gameWallbreaker.implementation.actors.Brick
import com.pandulapeter.kubriko.gameWallbreaker.implementation.actors.Paddle
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.WallbreakerScoreManager
import com.pandulapeter.kubriko.implementation.extensions.Invisible
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
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

internal class WallbreakerGameManager(
    private val stateManager: StateManager,
) : Manager(), KeyboardInputAware, Unique, Group {

    private lateinit var actorManager: ActorManager
    private lateinit var metadataManager: MetadataManager
    private lateinit var scoreManager: WallbreakerScoreManager

    @Composable
    override fun getModifier(layerIndex: Int?) = Modifier.pointerHoverIcon(
        icon = if (stateManager.isRunning.collectAsState().value) PointerIcon.Invisible else PointerIcon.Default
    )

    override val actors = listOf(
        SmoothPixelationShader(),
        VignetteShader(),
        ChromaticAberrationShader(),
    )

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        metadataManager = kubriko.require()
        scoreManager = kubriko.require()
        initializeScene()
        stateManager.isFocused
            .filterNot { it }
            .onEach { stateManager.updateIsRunning(false) }
            .launchIn(scope)
    }

    override fun onKeyReleased(key: Key) {
        if (key == Key.Escape) {
            stateManager.updateIsRunning(!stateManager.isRunning.value)
        }
    }

    // TODO: Only the bricks, the ball and the paddle hsould be reset
    private fun initializeScene() {
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
        actorManager.add(allBricks + Ball() + Paddle() + this)
    }

    fun pauseGame() = stateManager.updateIsRunning(false)

    fun resumeGame() = stateManager.updateIsRunning(true)

    fun restartGame() {
        actorManager.remove(actorManager.allActors.value.let { it.filterIsInstance<Brick>() + it.filterIsInstance<Ball>() + it.filterIsInstance<Paddle>() })
        initializeScene()
        scoreManager.resetScore()
        resumeGame()
    }
}