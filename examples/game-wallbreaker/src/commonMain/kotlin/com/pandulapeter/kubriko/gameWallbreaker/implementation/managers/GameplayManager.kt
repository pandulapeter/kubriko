/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameWallbreaker.implementation.managers

import com.pandulapeter.kubriko.Kubriko
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

internal class GameplayManager(
    private val stateManager: StateManager,
) : Manager() {

    private val actorManager by manager<ActorManager>()
    private val audioManager by manager<AudioManager>()
    private val scoreManager by manager<ScoreManager>()
    private val uiManager by manager<UIManager>()
    private val paddle = Paddle()
    private val _isGameOver = MutableStateFlow(true)
    val isGameOver = _isGameOver.asStateFlow()
    val isGameStarted get() = actorManager.allActors.value.filterIsInstance<Ball>().firstOrNull()?.isLaunched == true
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
    private var destroyedBrickCount = 0

    override fun onInitialize(kubriko: Kubriko) {
        actorManager.add(
            paddle,
            SmoothPixelationShader(),
            VignetteShader(),
            ChromaticAberrationShader(),
            uiManager,
        )
        startLevel()
    }

    private fun startLevel() {
        _isGameOver.value = false
        destroyedBrickCount = 0
        bricks.forEach {
            it.isDestroyed = false
            it.randomizeHue()
        }
        actorManager.add(bricks + Ball(paddle))
    }

    fun onBrickDestroyed() {
        destroyedBrickCount++
        if (destroyedBrickCount == bricks.size) onLevelCleared()
    }

    fun pauseGame() {
        audioManager.playClickSoundEffect()
        stateManager.updateIsRunning(false)
    }

    private fun onLevelCleared() {
        audioManager.playLevelClearedSoundEffect()
        actorManager.remove(actorManager.allActors.value.filterIsInstance<Ball>())
        startLevel()
    }

    fun onGameOver() {
        _isGameOver.value = true
        stateManager.updateIsRunning(false)
    }

    fun resumeGame() {
        if (uiManager.isInfoDialogVisible.value) {
            uiManager.onInfoDialogClosed()
        } else if (uiManager.isCloseConfirmationDialogVisible.value) {
            uiManager.onCloseConfirmationToggled()
        } else {
            audioManager.playClickSoundEffect()
            paddle.resetPointerTracking()
            stateManager.updateIsRunning(true)
        }
    }

    fun restartGame() {
        if (uiManager.isInfoDialogVisible.value) {
            uiManager.onInfoDialogClosed()
        } else if (uiManager.isCloseConfirmationDialogVisible.value) {
            uiManager.onCloseConfirmationToggled()
        } else {
            actorManager.remove(bricks + actorManager.allActors.value.filterIsInstance<Ball>())
            startLevel()
            scoreManager.resetScore()
            resumeGame()
        }
    }
}