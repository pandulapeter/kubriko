/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameWallbreaker.implementation.managers

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.extensions.Invisible
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ui.PauseMenuBackground
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputAware
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
    private val gameManager by manager<GameplayManager>()
    private val _isInfoDialogVisible = MutableStateFlow(false)
    val isInfoDialogVisible = _isInfoDialogVisible.asStateFlow()
    private val _isCloseConfirmationDialogVisible = MutableStateFlow(false)
    val isCloseConfirmationDialogVisible = _isCloseConfirmationDialogVisible.asStateFlow()

    @Composable
    override fun processModifier(modifier: Modifier, layerIndex: Int?) = modifier.pointerHoverIcon(
        icon = if (stateManager.isRunning.collectAsState().value) PointerIcon.Invisible else PointerIcon.Default
    )

    override fun onInitialize(kubriko: Kubriko) {
        stateManager.isFocused
            .filterNot { it }
            .onEach { stateManager.updateIsRunning(false) }
            .launchIn(scope)
    }

    override fun onKeyReleased(key: Key) {
        when (key) {
            Key.Spacebar, Key.Enter -> {
                if (!stateManager.isRunning.value && !isInfoDialogVisible.value) {
                    if (gameManager.isGameOver.value) {
                        gameManager.restartGame()
                    } else {
                        gameManager.resumeGame()
                    }
                }
            }

            else -> Unit
        }
    }

    @Composable
    override fun Composable(windowInsets: WindowInsets) = PauseMenuBackground(
        isVisible = !stateManager.isRunning.collectAsState().value,
    )

    fun toggleInfoDialogVisibility() = _isInfoDialogVisible.update { !it.also { if (it) audioManager.playClickSoundEffect() } }

    fun toggleCloseConfirmationDialogVisibility() = _isCloseConfirmationDialogVisible.update { !it.also { audioManager.playClickSoundEffect() } }
}