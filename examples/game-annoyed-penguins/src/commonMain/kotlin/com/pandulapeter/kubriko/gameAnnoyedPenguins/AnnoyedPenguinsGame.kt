/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
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
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.ui.AnnoyedPenguinsButton
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.ui.AnnoyedPenguinsTheme
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.ui.MenuOverlay
import kotlinx.collections.immutable.toImmutableList
import kubriko.examples.game_annoyed_penguins.generated.resources.Res
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_pause
import kubriko.examples.game_annoyed_penguins.generated.resources.pause
import org.jetbrains.compose.resources.stringResource

fun createAnnoyedPenguinsGameStateHolder(): AnnoyedPenguinsGameStateHolder = AnnoyedPenguinsGameStateHolderImpl()

@Composable
fun AnnoyedPenguinsGame(
    modifier: Modifier = Modifier,
    stateHolder: AnnoyedPenguinsGameStateHolder = createAnnoyedPenguinsGameStateHolder(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    isInFullscreenMode: Boolean? = null,
    onFullscreenModeToggled: () -> Unit = {},
) = AnnoyedPenguinsTheme {
    stateHolder as AnnoyedPenguinsGameStateHolderImpl
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
        AnimatedVisibility(
            visible = !isLoadingLevel,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            val gameAlpha by animateFloatAsState(
                targetValue = if (isGameRunning) 1f else 0.1f,
                animationSpec = tween(),
            )
            KubrikoViewport(
                modifier = Modifier.alpha(gameAlpha),
                kubriko = stateHolder.kubriko.value,
                windowInsets = windowInsets,
            )
        }
        AnimatedVisibility(
            visible = isGameRunning,
            enter = slideIn { IntOffset(0, -it.height) },
            exit = slideOut { IntOffset(0, -it.height) },
        ) {
            AnnoyedPenguinsButton(
                modifier = Modifier
                    .windowInsetsPadding(windowInsets)
                    .padding(16.dp),
                onButtonPressed = {
                    stateHolder.audioManager.playButtonToggleSoundEffect()
                    stateHolder.stateManager.updateIsRunning(false)
                },
                icon = Res.drawable.ic_pause,
                title = stringResource(Res.string.pause),
                onPointerEnter = stateHolder.audioManager::playButtonHoverSoundEffect,
            )
        }
        AnimatedVisibility(
            visible = !isGameRunning,
            enter = slideIn { IntOffset(0, -it.height) },
            exit = slideOut { IntOffset(0, -it.height) },
        ) {
            BoxWithConstraints {
                MenuOverlay(
                    modifier = Modifier.windowInsetsPadding(windowInsets),
                    currentLevel = stateHolder.gameplayManager.currentLevel.collectAsState().value,
                    allLevels = GameplayManager.AllLevels.keys.toImmutableList(),
                    onInfoButtonPressed = {
                        stateHolder.uiManager.toggleInfoDialogVisibility()
                        stateHolder.audioManager.playButtonToggleSoundEffect()
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
                    onLevelSelected = { level ->
                        stateHolder.audioManager.playButtonToggleSoundEffect()
                        stateHolder.gameplayManager.setCurrentLevel(level)
                        stateHolder.stateManager.updateIsRunning(true)
                    },
                    shouldUseLandscapeLayout = maxWidth > maxHeight,
                )
            }
        }
    }
    AnimatedVisibility(
        modifier = modifier,
        visible = !isGameLoaded || isLoadingLevel,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .padding(16.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.BottomStart).size(24.dp),
                strokeWidth = 3.dp,
            )
        }
    }
}