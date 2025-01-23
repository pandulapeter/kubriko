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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.extensions.Invisible
import com.pandulapeter.kubriko.extensions.get
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

internal class UIManager(
    private val stateManager: StateManager,
) : Manager(), KeyboardInputAware, Unique {

    private val audioManager by manager<AudioManager>()
    private val gameplayManager by manager<GameplayManager>()
    private val _isInfoDialogVisible = MutableStateFlow(false)
    val isInfoDialogVisible = _isInfoDialogVisible.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        kubriko.get<ActorManager>().add(this)
        stateManager.isFocused
            .filterNot { it }
            .onEach { gameplayManager.pauseGame() }
            .launchIn(scope)
    }

    @Composable
    override fun processModifier(modifier: Modifier, layerIndex: Int?) = modifier.pointerHoverIcon(
        icon = if (stateManager.isRunning.collectAsState().value) PointerIcon.Invisible else PointerIcon.Default
    )

    override fun onKeyReleased(key: Key) {
        when (key) {
            Key.Escape -> if (stateManager.isRunning.value) {
                gameplayManager.pauseGame()
            } else {
                if (isInfoDialogVisible.value) {
                    toggleInfoDialogVisibility()
                } else {
                    gameplayManager.playGame()
                }
            }

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