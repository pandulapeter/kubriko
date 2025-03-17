/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameBlockysJourney

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.BlockysJourneyGameStateHolder
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.BlockysJourneyGameStateHolderImpl
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.ui.BlockysJourneyButton
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.ui.BlockysJourneyTheme
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.ui.MenuOverlay
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.ui.UnfinishedDisclaimer
import kubriko.examples.game_blockys_journey.generated.resources.Res
import kubriko.examples.game_blockys_journey.generated.resources.ic_pause
import kubriko.examples.game_blockys_journey.generated.resources.pause
import org.jetbrains.compose.resources.stringResource

fun createBlockysJourneyGameStateHolder(
    webRootPathName: String,
    isSceneEditorEnabled: Boolean,
    isLoggingEnabled: Boolean,
): BlockysJourneyGameStateHolder = BlockysJourneyGameStateHolderImpl(
    webRootPathName = webRootPathName,
    isSceneEditorEnabled = isSceneEditorEnabled,
    isLoggingEnabled = isLoggingEnabled,
)

@Composable
fun BlockysJourneyGame(
    modifier: Modifier = Modifier,
    stateHolder: BlockysJourneyGameStateHolder = createBlockysJourneyGameStateHolder(
        webRootPathName = "",
        isSceneEditorEnabled = true,
        isLoggingEnabled = false,
    ),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    isInFullscreenMode: Boolean? = null,
    onFullscreenModeToggled: () -> Unit = {},
) = BlockysJourneyTheme {
    stateHolder as BlockysJourneyGameStateHolderImpl
    KubrikoViewport(
        modifier = modifier.fillMaxSize().background(Color.Black),
        kubriko = stateHolder.backgroundKubriko,
        windowInsets = windowInsets,
    )
    val isGameLoaded = stateHolder.sharedLoadingManager.isGameLoaded()
    val isGameRunning = stateHolder.stateManager.isRunning.collectAsState().value
    val isGameFocused = stateHolder.stateManager.isFocused.collectAsState().value
    AnimatedVisibility(
        visible = isGameLoaded,
        enter = fadeIn() + scaleIn(initialScale = 0.88f),
        exit = scaleOut(targetScale = 0.88f) + fadeOut(),
    ) {
        val gameAlpha by animateFloatAsState(
            targetValue = if (isGameRunning) 1f else 0.2f,
            animationSpec = tween(),
        )
        KubrikoViewport(
            modifier = Modifier.alpha(gameAlpha),
            kubriko = stateHolder.kubriko.value,
            windowInsets = windowInsets,
        )
        AnimatedVisibility(
            visible = isGameRunning,
            enter = slideIn { IntOffset(0, -it.height) } + fadeIn(),
            exit = fadeOut() + slideOut { IntOffset(0, -it.height) },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .windowInsetsPadding(windowInsets)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BlockysJourneyButton(
                    onButtonPressed = {
                        stateHolder.audioManager.playButtonToggleSoundEffect()
                        stateHolder.stateManager.updateIsRunning(false)
                    },
                    icon = Res.drawable.ic_pause,
                    title = stringResource(Res.string.pause),
                    onPointerEnter = stateHolder.audioManager::playButtonHoverSoundEffect,
                )
                UnfinishedDisclaimer(
                    modifier = Modifier.weight(1f),
                )
            }
        }
        AnimatedVisibility(
            visible = !isGameRunning,
            enter = slideIn { IntOffset(0, -it.height) } + fadeIn(),
            exit = fadeOut() + slideOut { IntOffset(0, -it.height) },
        ) {
            MenuOverlay(
                windowInsets = windowInsets,
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
                onPlayButtonPressed = {
                    stateHolder.audioManager.playButtonToggleSoundEffect()
                    stateHolder.stateManager.updateIsRunning(true)
                },
                isSceneEditorEnabled = stateHolder.isSceneEditorEnabled,
            )
        }
    }
    AnimatedVisibility(
        modifier = modifier,
        visible = !isGameLoaded,
        enter = EnterTransition.None,
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