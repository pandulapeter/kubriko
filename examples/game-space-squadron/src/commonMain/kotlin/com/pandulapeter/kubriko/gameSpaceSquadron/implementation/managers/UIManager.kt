/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.Ship
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui.ScoreIndicator
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui.ShipStatusBars
import com.pandulapeter.kubriko.helpers.extensions.Invisible
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class UIManager(
    private val stateManager: StateManager,
) : Manager(), KeyboardInputAware, Unique {

    private val actorManager by manager<ActorManager>()
    private val audioManager by manager<AudioManager>()
    private val keyboardInputManager by manager<KeyboardInputManager>()
    private val gameplayManager by manager<GameplayManager>()
    private val scoreManager by manager<ScoreManager>()
    private val _isInfoDialogVisible = MutableStateFlow(false)
    val isInfoDialogVisible = _isInfoDialogVisible.asStateFlow()
    private val _isCloseConfirmationDialogVisible = MutableStateFlow(false)
    val isCloseConfirmationDialogVisible = _isCloseConfirmationDialogVisible.asStateFlow()
    private val shipHealth = MutableStateFlow(0)
    private val multiShoot = MutableStateFlow(0)
    private var shouldDismissNextSpacebarRelease = false

    override fun onInitialize(kubriko: Kubriko) {
        actorManager.add(this)
        stateManager.isFocused
            .filterNot { it }
            .onEach { gameplayManager.pauseGame() }
            .launchIn(scope)
        gameplayManager.isGameOver
            .filter { it }
            .onEach {
                if (keyboardInputManager.isKeyPressed(Key.Spacebar)) {
                    shouldDismissNextSpacebarRelease = true
                }
            }
            .launchIn(scope)
    }

    fun updateShipHealth(shipHealth: Int) = this.shipHealth.update { shipHealth }

    fun updateShipMultiShoot(multiShoot: Int) = this.multiShoot.update { multiShoot }

    @Composable
    override fun processModifier(modifier: Modifier, layerIndex: Int?, gameTime: State<Long>) = modifier.pointerHoverIcon(
        icon = if (stateManager.isRunning.collectAsState().value && !gameplayManager.isGameOver.collectAsState().value) PointerIcon.Invisible else PointerIcon.Default
    )

    @Composable
    override fun Composable(windowInsets: WindowInsets) = Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(windowInsets),
    ) {
        AnimatedVisibility(
            enter = fadeIn() + slideIn { IntOffset(0, it.height) },
            exit = slideOut { IntOffset(0, it.height) } + fadeOut(),
            visible = stateManager.isRunning.collectAsState().value && !gameplayManager.isGameOver.collectAsState().value,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                ShipStatusBars(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .align(Alignment.BottomEnd),
                    healthFraction = shipHealth.collectAsState().value / Ship.MAX_HEALTH.toFloat(),
                    multiShootFraction = multiShoot.collectAsState().value / Ship.MAX_MULTI_SHOOT.toFloat(),
                )
            }
        }
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomStart),
            enter = fadeIn() + slideIn { IntOffset(0, it.height) },
            exit = slideOut { IntOffset(0, it.height) } + fadeOut(),
            visible = scoreManager.score.collectAsState().value > 0 && !isInfoDialogVisible.collectAsState().value && !isCloseConfirmationDialogVisible.collectAsState().value,
        ) {
            ScoreIndicator(
                highScore = scoreManager.highScore.collectAsState().value,
                score = scoreManager.score.collectAsState().value,
            )
        }
    }

    override fun onKeyReleased(key: Key) {
        when (key) {
            Key.Spacebar, Key.Enter -> {
                if (!shouldDismissNextSpacebarRelease && (!stateManager.isRunning.value || gameplayManager.isGameOver.value) && !isInfoDialogVisible.value && !isCloseConfirmationDialogVisible.value) {
                    gameplayManager.playGame()
                }
                shouldDismissNextSpacebarRelease = false
            }

            else -> Unit
        }
    }

    fun toggleInfoDialogVisibility() = _isInfoDialogVisible.update { !it.also { if (it) audioManager.playButtonToggleSoundEffect() } }

    fun toggleCloseConfirmationDialogVisibility() = _isCloseConfirmationDialogVisible.update { !it.also { audioManager.playButtonToggleSoundEffect() } }
}