/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.extensions.Invisible
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.Ship
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui.SpaceSquadronUIElementShape
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui.spaceSquadronUIElementBorder
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kubriko.examples.game_space_squadron.generated.resources.Res
import kubriko.examples.game_space_squadron.generated.resources.score
import org.jetbrains.compose.resources.stringResource

internal class UIManager(
    private val stateManager: StateManager,
) : Manager(), KeyboardInputAware, Unique {

    private val audioManager by manager<AudioManager>()
    private val gameplayManager by manager<GameplayManager>()
    private val scoreManager by manager<ScoreManager>()
    private val _isInfoDialogVisible = MutableStateFlow(false)
    val isInfoDialogVisible = _isInfoDialogVisible.asStateFlow()
    private val shipHealth = MutableStateFlow(0)
    private val multiShoot = MutableStateFlow(0)

    override fun onInitialize(kubriko: Kubriko) {
        kubriko.get<ActorManager>().add(this)
        stateManager.isFocused
            .filterNot { it }
            .onEach { gameplayManager.pauseGame() }
            .launchIn(scope)
    }

    fun updateShipHealth(shipHealth: Int) = this.shipHealth.update { shipHealth }

    fun updateShipMultiShoot(multiShoot: Int) = this.multiShoot.update { multiShoot }

    @Composable
    override fun processModifier(modifier: Modifier, layerIndex: Int?) = modifier.pointerHoverIcon(
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .align(Alignment.BottomStart),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ProgressBar(
                        value = shipHealth.collectAsState().value / Ship.MAX_HEALTH.toFloat(),
                        minColor = Color.Red,
                        maxColor = Color.Magenta,
                    )
                    ProgressBar(
                        value = multiShoot.collectAsState().value / Ship.MAX_MULTI_SHOOT.toFloat(),
                        minColor = Color.Red,
                        maxColor = Color.Cyan,
                    )
                }
            }
        }
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.TopEnd),
            enter = fadeIn() + slideIn { IntOffset(0, -it.height) },
            exit = slideOut { IntOffset(0, -it.height) } + fadeOut(),
            visible = scoreManager.score.collectAsState().value > 0 || stateManager.isRunning.collectAsState().value,
        ) {
            Text(
                modifier = Modifier
                    .padding(16.dp)
                    .spaceSquadronUIElementBorder()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp,
                    ),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                text = stringResource(Res.string.score, scoreManager.highScore.collectAsState().value, scoreManager.score.collectAsState().value),
            )
        }
    }

    @Composable
    private fun ProgressBar(
        value: Float,
        minColor: Color,
        maxColor: Color,
    ) = Box(
        modifier = Modifier
            .fillMaxWidth()
            .spaceSquadronUIElementBorder(),
    ) {
        val animatedValue = animateFloatAsState(
            targetValue = value,
            animationSpec = tween(),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = animatedValue.value)
                .height(12.dp)
                .clip(SpaceSquadronUIElementShape)
                .background(lerp(minColor, maxColor, animatedValue.value).copy(alpha = 0.5f)),
        )
    }

    override fun onKeyReleased(key: Key) {
        when (key) {
            Key.Spacebar, Key.Enter -> {
                if (!stateManager.isRunning.value && !isInfoDialogVisible.value) {
                    gameplayManager.playGame()
                }
            }

            else -> Unit
        }
    }

    fun toggleInfoDialogVisibility() = _isInfoDialogVisible.update { !it.also { if (it) audioManager.playButtonToggleSoundEffect() } }
}