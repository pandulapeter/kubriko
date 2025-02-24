/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.SpaceSquadronGameStateHolder
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.SpaceSquadronGameStateHolderImpl
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui.SpaceSquadronMenuOverlay
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui.SpaceSquadronTheme

fun createSpaceSquadronGameStateHolder(): SpaceSquadronGameStateHolder = SpaceSquadronGameStateHolderImpl()

@Composable
fun SpaceSquadronGame(
    modifier: Modifier = Modifier,
    stateHolder: SpaceSquadronGameStateHolder = createSpaceSquadronGameStateHolder(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    isInFullscreenMode: Boolean? = null,
    onFullscreenModeToggled: () -> Unit = {},
) = SpaceSquadronTheme {
    stateHolder as SpaceSquadronGameStateHolderImpl
    val isGameLoaded = stateHolder.backgroundLoadingManager.isGameLoaded()
    KubrikoViewport(
        modifier = modifier.fillMaxSize().background(Color.Black),
        kubriko = stateHolder.backgroundKubriko,
    )
    AnimatedVisibility(
        modifier = modifier,
        visible = !isGameLoaded,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .windowInsetsPadding(windowInsets)
                .padding(16.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.BottomStart).size(24.dp),
                strokeWidth = 3.dp,
            )
        }
    }
    AnimatedVisibility(
        visible = isGameLoaded,
        enter = fadeIn() + scaleIn(initialScale = 0.88f),
        exit = scaleOut(targetScale = 0.88f) + fadeOut(),
    ) {
        KubrikoViewport(
            kubriko = stateHolder.kubriko.collectAsState().value,
            windowInsets = windowInsets,
        )
        AnimatedVisibility(
            visible = stateHolder.uiManager.isCloseConfirmationDialogVisible.collectAsState().value,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.5f)),
            )
        }
        AnimatedVisibility(
            visible = isGameLoaded,
            enter = fadeIn() + scaleIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            SpaceSquadronMenuOverlay(
                modifier = Modifier.windowInsetsPadding(windowInsets),
                isVisible = !stateHolder.stateManager.isRunning.collectAsState().value || stateHolder.gameplayManager.isGameOver.collectAsState().value,
                shouldShowInfoText = stateHolder.uiManager.isInfoDialogVisible.collectAsState().value,
                shouldCloseConfirmationDialog = stateHolder.uiManager.isCloseConfirmationDialogVisible.collectAsState().value,
                onPlayButtonPressed = stateHolder.gameplayManager::playGame,
                onLeaveButtonPressed = stateHolder.uiManager::toggleCloseConfirmationDialogVisibility,
                onCloseConfirmed = {
                    stateHolder.audioManager.playButtonToggleSoundEffect()
                    stateHolder.backNavigationIntent.tryEmit(Unit)
                },
                onPauseButtonPressed = stateHolder.gameplayManager::pauseGame,
                onInfoButtonPressed = {
                    stateHolder.audioManager.playButtonToggleSoundEffect()
                    stateHolder.uiManager.toggleInfoDialogVisibility()
                },
                areSoundEffectsEnabled = stateHolder.stateManager.isFocused.collectAsState().value && stateHolder.userPreferencesManager.areSoundEffectsEnabled.collectAsState().value,
                onSoundEffectsToggled = stateHolder.userPreferencesManager::onAreSoundEffectsEnabledChanged,
                isMusicEnabled = stateHolder.stateManager.isFocused.collectAsState().value && stateHolder.userPreferencesManager.isMusicEnabled.collectAsState().value,
                onMusicToggled = stateHolder.userPreferencesManager::onIsMusicEnabledChanged,
                isInFullscreenMode = isInFullscreenMode,
                onFullscreenModeToggled = {
                    stateHolder.audioManager.playButtonToggleSoundEffect()
                    onFullscreenModeToggled()
                },
                onButtonHover = stateHolder.audioManager::playButtonHoverSoundEffect,
            )
        }
    }
}