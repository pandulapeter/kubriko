/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameWallbreaker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
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
import com.pandulapeter.kubriko.gameWallbreaker.implementation.WallbreakerGameStateHolder
import com.pandulapeter.kubriko.gameWallbreaker.implementation.WallbreakerGameStateHolderImpl
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ui.CloseConfirmationDialogOverlay
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ui.GameOverlay
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ui.InfoDialogOverlay
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ui.MenuOverlay
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ui.WallbreakerTheme

fun createWallbreakerGameStateHolder(
    webRootPathName: String,
    isLoggingEnabled: Boolean,
): WallbreakerGameStateHolder = WallbreakerGameStateHolderImpl(
    webRootPathName = webRootPathName,
    isLoggingEnabled = isLoggingEnabled,
)

@Composable
fun WallbreakerGame(
    modifier: Modifier = Modifier,
    stateHolder: WallbreakerGameStateHolder = createWallbreakerGameStateHolder(
        webRootPathName = "",
        isLoggingEnabled = false,
    ),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    isInFullscreenMode: Boolean? = null,
    onFullscreenModeToggled: () -> Unit = {},
) = WallbreakerTheme {
    stateHolder as WallbreakerGameStateHolderImpl
    val isGameRunning = stateHolder.stateManager.isRunning.collectAsState().value
    val isGameLoaded = stateHolder.backgroundLoadingManager.isGameLoaded()
    KubrikoViewport(
        modifier = modifier
            .fillMaxSize()
            .background(if (stateHolder.shaderManager.areShadersSupported) Color.Black else Color.DarkGray),
        kubriko = stateHolder.backgroundKubriko,
    )
    AnimatedVisibility(
        modifier = modifier,
        visible = !isGameLoaded,
        enter = EnterTransition.None,
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(windowInsets).padding(16.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.BottomStart).size(24.dp),
                strokeWidth = 3.dp,
            )
        }
    }
    AnimatedVisibility(
        modifier = modifier,
        visible = isGameLoaded,
        enter = fadeIn() + scaleIn(initialScale = 0.88f),
        exit = scaleOut(targetScale = 0.88f) + fadeOut(),
    ) {
        KubrikoViewport(
            modifier = Modifier.windowInsetsPadding(windowInsets).background(Color.White.copy(alpha = 0.05f)),
            kubriko = stateHolder.kubriko.collectAsState().value,
            windowInsets = windowInsets,
        )
        val isFocused = stateHolder.stateManager.isFocused.collectAsState().value
        val isInfoDialogVisible = stateHolder.uiManager.isInfoDialogVisible.collectAsState().value
        val isCloseConfirmationDialogVisible = stateHolder.uiManager.isCloseConfirmationDialogVisible.collectAsState().value
        GameOverlay(
            gameAreaModifier = Modifier.fillMaxSize().windowInsetsPadding(windowInsets),
            isGameRunning = isGameRunning,
            score = stateHolder.scoreManager.score.collectAsState().value,
            highScore = stateHolder.scoreManager.highScore.collectAsState().value,
            onPauseButtonPressed = stateHolder.gameplayManager::pauseGame,
            onButtonHover = stateHolder.audioManager::playHoverSoundEffect,
        )
        MenuOverlay(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(windowInsets),
            isVisible = !isGameRunning,
            isActive = !isInfoDialogVisible && !isCloseConfirmationDialogVisible,
            shouldShowResumeButton = !stateHolder.gameplayManager.isGameOver.collectAsState().value,
            onResumeButtonPressed = stateHolder.gameplayManager::resumeGame,
            onRestartButtonPressed = stateHolder.gameplayManager::restartGame,
            onInfoButtonPressed = {
                stateHolder.audioManager.playClickSoundEffect()
                stateHolder.uiManager.toggleInfoDialogVisibility()
            },
            onExitButtonPressed = {
                stateHolder.audioManager.playClickSoundEffect()
                stateHolder.uiManager.toggleCloseConfirmationDialogVisibility()
            },
            areSoundEffectsEnabled = isFocused && stateHolder.userPreferencesManager.areSoundEffectsEnabled.collectAsState().value,
            onSoundEffectsToggled = stateHolder.userPreferencesManager::onAreSoundEffectsEnabledChanged,
            isMusicEnabled = isFocused && stateHolder.userPreferencesManager.isMusicEnabled.collectAsState().value,
            onMusicToggled = stateHolder.userPreferencesManager::onIsMusicEnabledChanged,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = {
                stateHolder.audioManager.playClickSoundEffect()
                onFullscreenModeToggled()
            },
            onButtonHover = {
                if (!stateHolder.stateManager.isRunning.value) {
                    stateHolder.audioManager.playHoverSoundEffect()
                }
            },
        )
        InfoDialogOverlay(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(windowInsets),
            isVisible = isInfoDialogVisible,
            onInfoDialogClosed = stateHolder.uiManager::toggleInfoDialogVisibility,
            onButtonHover = stateHolder.audioManager::playHoverSoundEffect,
        )
        CloseConfirmationDialogOverlay(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(windowInsets),
            isVisible = isCloseConfirmationDialogVisible,
            onCloseConfirmed = {
                stateHolder.audioManager.playClickSoundEffect()
                stateHolder.backNavigationIntent.tryEmit(Unit)
            },
            onCloseCancelled = stateHolder.uiManager::toggleCloseConfirmationDialogVisibility,
            onButtonHover = stateHolder.audioManager::playHoverSoundEffect,
        )
    }
}