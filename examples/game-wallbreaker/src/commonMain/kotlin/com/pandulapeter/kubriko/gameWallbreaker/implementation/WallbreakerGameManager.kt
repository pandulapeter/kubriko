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
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.WallbreakerAudioManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.WallbreakerScoreManager
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.shader.collection.ChromaticAberrationShader
import com.pandulapeter.kubriko.shader.collection.SmoothPixelationShader
import com.pandulapeter.kubriko.shader.collection.VignetteShader
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class WallbreakerGameManager(
    private val stateManager: StateManager,
) : Manager(), KeyboardInputAware, Unique, Group {

    private val actorManager by manager<ActorManager>()
    private val audioManager by manager<WallbreakerAudioManager>()
    private val pointerInputManager by manager<PointerInputManager>()
    private val scoreManager by manager<WallbreakerScoreManager>()
    private val paddle = Paddle()
    private val _isGameOver = MutableStateFlow(true)
    val isGameOver = _isGameOver.asStateFlow()

    override val actors = listOf(
        paddle,
        SmoothPixelationShader(),
        VignetteShader(),
        ChromaticAberrationShader(),
    )

    @Composable
    override fun getModifier(layerIndex: Int?) = Modifier.pointerHoverIcon(
        icon = if (stateManager.isRunning.collectAsState().value) PointerIcon.Crosshair else PointerIcon.Default
    )

    override fun onInitialize(kubriko: Kubriko) {
        initializeScene()
        stateManager.isFocused
            .filterNot { it }
            .onEach { stateManager.updateIsRunning(false) }
            .launchIn(scope)
        stateManager.isRunning
            .onEach { pointerInputManager.pointerScreenOffset.value?.let(paddle::onPointerOffsetChanged) }
            .launchIn(scope)
        scoreManager.score
            .onEach { if (actorManager.allActors.value.filterIsInstance<Brick>().isEmpty()) onLevelCleared() }
            .launchIn(scope)
    }

    override fun onKeyReleased(key: Key) {
        if (key == Key.Escape) {
            stateManager.updateIsRunning(!stateManager.isRunning.value)
            if (isGameOver.value) {
                restartGame()
            }
        }
    }

    // TODO: Only the bricks, the ball and the paddle hsould be reset
    private fun initializeScene() {
        _isGameOver.value = false
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
        actorManager.add(allBricks + Ball(paddle) + this)
    }

    fun pauseGame() = stateManager.updateIsRunning(false)

    private fun onLevelCleared() {
        audioManager.playLevelClearedSoundEffect()
        actorManager.remove(actorManager.allActors.value.let { it.filterIsInstance<Ball>() + it.filterIsInstance<Paddle>() })
        initializeScene()
    }

    fun onGameOver() {
        _isGameOver.value = true
        stateManager.updateIsRunning(false)
    }

    fun resumeGame() = stateManager.updateIsRunning(true)

    fun restartGame() {
        actorManager.remove(actorManager.allActors.value.let { it.filterIsInstance<Brick>() + it.filterIsInstance<Ball>() + it.filterIsInstance<Paddle>() })
        initializeScene()
        scoreManager.resetScore()
        resumeGame()
    }
}