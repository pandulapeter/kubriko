package com.pandulapeter.kubriko.gameWallbreaker.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Group
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.gameWallbreaker.implementation.actors.Ball
import com.pandulapeter.kubriko.gameWallbreaker.implementation.actors.Brick
import com.pandulapeter.kubriko.gameWallbreaker.implementation.actors.Paddle
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.shaders.collection.ChromaticAberrationShader
import com.pandulapeter.kubriko.shaders.collection.SmoothPixelationShader
import com.pandulapeter.kubriko.shaders.collection.VignetteShader
import com.pandulapeter.kubriko.types.SceneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class GameplayManager(
    private val stateManager: StateManager,
) : Manager(), Unique, Group {

    private val actorManager by manager<ActorManager>()
    private val audioManager by manager<AudioManager>()
    private val scoreManager by manager<ScoreManager>()
    private val uiManager by manager<UIManager>()
    private val paddle = Paddle()
    private val _isGameOver = MutableStateFlow(true)
    val isGameOver = _isGameOver.asStateFlow()
    private val bricks = (-8..1).flatMap { y ->
        (-4..4).map { x ->
            Brick(
                position = SceneOffset(
                    x = Brick.Width * x,
                    y = Brick.Height * y,
                ),
            )
        }
    }
    override val actors by lazy {
        bricks + listOf(
            paddle,
            SmoothPixelationShader(),
            VignetteShader(),
            ChromaticAberrationShader(),
            uiManager,
        )
    }

    override fun onInitialize(kubriko: Kubriko) {
        initializeScene()
        scoreManager.score
            .onEach { if (actorManager.allActors.value.filterIsInstance<Brick>().isEmpty()) onLevelCleared() }
            .launchIn(scope)
    }

    private fun initializeScene() {
        _isGameOver.value = false
        bricks.forEach { it.randomizeHue() }
        actorManager.add(listOf(Ball(paddle), this))
    }

    fun pauseGame() {
        audioManager.playClickSoundEffect()
        stateManager.updateIsRunning(false)
    }

    private fun onLevelCleared() {
        audioManager.playLevelClearedSoundEffect()
        actorManager.remove(actorManager.allActors.value.let { it.filterIsInstance<Ball>() + it.filterIsInstance<Paddle>() })
        initializeScene()
    }

    fun onGameOver() {
        _isGameOver.value = true
        stateManager.updateIsRunning(false)
    }

    fun resumeGame() {
        if (uiManager.isInfoDialogVisible.value) {
            uiManager.onInfoDialogVisibilityChanged()
        } else {
            audioManager.playClickSoundEffect()
            paddle.resetPointerTracking()
            stateManager.updateIsRunning(true)
        }
    }

    fun restartGame() {
        if (uiManager.isInfoDialogVisible.value) {
            uiManager.onInfoDialogVisibilityChanged()
        } else {
            actorManager.remove(actorManager.allActors.value.let { it.filterIsInstance<Brick>() + it.filterIsInstance<Ball>() + it.filterIsInstance<Paddle>() })
            initializeScene()
            scoreManager.resetScore()
            resumeGame()
        }
    }
}