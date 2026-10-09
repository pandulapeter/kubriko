/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.AnnoyedPenguinsGameStateHolder
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.AnnoyedPenguinsGameStateHolderImpl
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.managers.GameplayManager
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.ui.AnnoyedPenguinsTheme
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.ui.GameplayHud
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.ui.MenuOverlay
import com.pandulapeter.kubriko.uiComponents.LoadingIndicator

@Composable
fun AnnoyedPenguinsGame(
    stateHolder: AnnoyedPenguinsGameStateHolder,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    isInFullscreenMode: Boolean? = null,
    onFullscreenModeToggled: () -> Unit = {},
) = AnnoyedPenguinsTheme {
    stateHolder as AnnoyedPenguinsGameStateHolderImpl
    // Touching kubriko first creates the instance that initializes stateManager.
    val kubriko = stateHolder.kubriko.collectAsState().value
    KubrikoViewport(
        modifier = modifier.fillMaxSize().background(Color(0xff6bbfc9)),
        kubriko = stateHolder.backgroundKubriko,
        windowInsets = windowInsets,
    )
    val isGameLoaded = stateHolder.backgroundLoadingManager.isGameLoaded()
    val isGameRunning = stateHolder.stateManager.isRunning.collectAsState().value
    val isGameFocused = stateHolder.stateManager.isFocused.collectAsState().value
    val isLoadingLevel = stateHolder.gameplayManager.isLoadingLevel.collectAsState().value
    AnimatedVisibility(
        visible = isGameLoaded,
        enter = fadeIn() + scaleIn(initialScale = 0.88f),
        exit = scaleOut(targetScale = 0.88f) + fadeOut(),
    ) {
        val loadingAlpha by animateFloatAsState(
            targetValue = if (isLoadingLevel) 0f else 1f,
        )
        val gameAlpha by animateFloatAsState(
            targetValue = if (isGameRunning) 1f else 0.5f,
            animationSpec = tween(),
        )
        KubrikoViewport(
            modifier = Modifier.alpha(loadingAlpha * gameAlpha * stateHolder.gameplayManager.gameViewportAlpha.collectAsState().value),
            kubriko = kubriko,
            windowInsets = windowInsets,
        )
        AnimatedVisibility(
            visible = isGameRunning && !isLoadingLevel,
            enter = slideIn { IntOffset(0, -it.height) },
            exit = slideOut { IntOffset(0, -it.height) },
        ) {
            GameplayHud(
                windowInsets = windowInsets,
                onPauseButtonPressed = {
                    stateHolder.audioManager.playButtonToggleSoundEffect()
                    stateHolder.stateManager.updateIsRunning(false)
                },
                onButtonHover = stateHolder.audioManager::playButtonHoverSoundEffect,
                minimumScaleFactor = stateHolder.viewportManager.minimumScaleFactor,
                maximumScaleFactor = stateHolder.viewportManager.maximumScaleFactor,
                currentScaleFactor = stateHolder.viewportManager.rawScaleFactor.collectAsState().value.vertical,
                onScaleFactorChanged = {
                    stateHolder.gameplayManager.onScaleFactorChanged()
                    stateHolder.viewportManager.setScaleFactor(it)
                },
                collectedStarCount = stateHolder.gameplayManager.collectedStarCount.collectAsState().value,
                totalStarCount = stateHolder.gameplayManager.totalStarCount.collectAsState().value,
            )
        }
        AnimatedVisibility(
            visible = !isGameRunning,
            enter = slideIn { IntOffset(0, -it.height) },
            exit = slideOut { IntOffset(0, -it.height) },
        ) {
            MenuOverlay(
                windowInsets = windowInsets,
                currentLevel = stateHolder.gameplayManager.currentLevel.collectAsState().value,
                allLevels = GameplayManager.LevelNames,
                onInfoButtonPressed = {
                    stateHolder.audioManager.playButtonToggleSoundEffect()
                    stateHolder.uiManager.toggleInfoDialogVisibility()
                },
                onCloseButtonPressed = {
                    stateHolder.audioManager.playButtonToggleSoundEffect()
                    stateHolder.uiManager.toggleCloseConfirmationDialogVisibility()
                },
                onCloseConfirmed = {
                    stateHolder.audioManager.playButtonToggleSoundEffect()
                    stateHolder.backNavigationIntent.tryEmit(Unit)
                },
                areSoundEffectsEnabled = isGameFocused && stateHolder.sharedUserPreferencesManager.areSoundEffectsEnabled.collectAsState().value,
                onSoundEffectsToggled = stateHolder.sharedUserPreferencesManager::onAreSoundEffectsEnabledChanged,
                isMusicEnabled = isGameFocused && stateHolder.sharedUserPreferencesManager.isMusicEnabled.collectAsState().value,
                onMusicToggled = stateHolder.sharedUserPreferencesManager::onIsMusicEnabledChanged,
                isInFullscreenMode = isInFullscreenMode,
                onFullscreenModeToggled = {
                    onFullscreenModeToggled()
                    stateHolder.audioManager.playButtonToggleSoundEffect()
                },
                playToggleSoundEffect = stateHolder.audioManager::playButtonToggleSoundEffect,
                playHoverSoundEffect = stateHolder.audioManager::playButtonHoverSoundEffect,
                isInfoDialogVisible = stateHolder.uiManager.isInfoDialogVisible.collectAsState().value,
                isCloseConfirmationDialogVisible = stateHolder.uiManager.isCloseConfirmationDialogVisible.collectAsState().value,
                onLevelSelected = { level ->
                    stateHolder.audioManager.playButtonToggleSoundEffect()
                    stateHolder.gameplayManager.setCurrentLevel(level)
                    stateHolder.stateManager.updateIsRunning(true)
                },
                isSceneEditorEnabled = stateHolder.isSceneEditorEnabled,
            )
        }
    }
    AnimatedVisibility(
        modifier = modifier,
        visible = !isGameLoaded || isLoadingLevel,
        enter = if (isLoadingLevel) fadeIn() else EnterTransition.None,
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .padding(16.dp),
        ) {
            LoadingIndicator(modifier = Modifier.align(Alignment.BottomStart))
        }
    }
}