/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kubriko.examples.game_annoyed_penguins.generated.resources.Res
import kubriko.examples.game_annoyed_penguins.generated.resources.close_confirmation_positive
import kubriko.examples.game_annoyed_penguins.generated.resources.fullscreen_enter
import kubriko.examples.game_annoyed_penguins.generated.resources.fullscreen_exit
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_exit
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_fullscreen_enter
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_fullscreen_exit
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_information
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_music_off
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_music_on
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_sound_effects_off
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_sound_effects_on
import kubriko.examples.game_annoyed_penguins.generated.resources.img_logo
import kubriko.examples.game_annoyed_penguins.generated.resources.information
import kubriko.examples.game_annoyed_penguins.generated.resources.music_disable
import kubriko.examples.game_annoyed_penguins.generated.resources.music_enable
import kubriko.examples.game_annoyed_penguins.generated.resources.resume
import kubriko.examples.game_annoyed_penguins.generated.resources.sound_effects_disable
import kubriko.examples.game_annoyed_penguins.generated.resources.sound_effects_enable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MenuOverlay(
    modifier: Modifier = Modifier,
    currentLevel: String?,
    allLevels: ImmutableList<String>,
    onInfoButtonPressed: () -> Unit,
    onCloseButtonPressed: () -> Unit,
    onCloseConfirmed: () -> Unit,
    areSoundEffectsEnabled: Boolean,
    onSoundEffectsToggled: () -> Unit,
    isMusicEnabled: Boolean,
    onMusicToggled: () -> Unit,
    isInFullscreenMode: Boolean?,
    onFullscreenModeToggled: () -> Unit,
    playToggleSoundEffect: () -> Unit = {},
    playHoverSoundEffect: () -> Unit = {},
    isInfoDialogVisible: Boolean,
    isCloseConfirmationDialogVisible: Boolean,
    onLevelSelected: (String) -> Unit,
    levelSelectorScrollState: ScrollState = rememberScrollState(),
) {
    AnimatedVisibility(
        visible = isInfoDialogVisible,
        enter = slideIn { IntOffset(0, -it.height) },
        exit = slideOut { IntOffset(0, -it.height) },
    ) {
        InfoDialog(
            modifier = modifier,
            onInfoButtonPressed = onInfoButtonPressed,
            onPointerEnter = playHoverSoundEffect,
        )
    }
    AnimatedVisibility(
        visible = !isInfoDialogVisible,
        enter = slideIn { IntOffset(0, it.height) },
        exit = slideOut { IntOffset(0, it.height) },
    ) {
        BoxWithConstraints {
            val shouldShowLogo = maxHeight > 256.dp
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp)
                        .weight(0.5f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AnnoyedPenguinsButton(
                        onButtonPressed = onCloseButtonPressed,
                        icon = Res.drawable.ic_exit,
                        title = stringResource(Res.string.close_confirmation_positive),
                        onPointerEnter = playHoverSoundEffect,
                    )
                    PlatformSpecificContent(
                        playHoverSoundEffect = playHoverSoundEffect,
                        playToggleSoundEffect = playToggleSoundEffect,
                    )
                    Spacer(
                        modifier = Modifier.weight(1f),
                    )
                    AnnoyedPenguinsButton(
                        icon = Res.drawable.ic_information,
                        title = stringResource(Res.string.information),
                        onButtonPressed = onInfoButtonPressed,
                        onPointerEnter = playHoverSoundEffect,
                    )
                    AnnoyedPenguinsButton(
                        onButtonPressed = onSoundEffectsToggled,
                        icon = if (areSoundEffectsEnabled) Res.drawable.ic_sound_effects_on else Res.drawable.ic_sound_effects_off,
                        title = stringResource(if (areSoundEffectsEnabled) Res.string.sound_effects_disable else Res.string.sound_effects_enable),
                        onPointerEnter = playHoverSoundEffect,
                    )
                    AnnoyedPenguinsButton(
                        onButtonPressed = onMusicToggled,
                        icon = if (isMusicEnabled) Res.drawable.ic_music_on else Res.drawable.ic_music_off,
                        title = stringResource(if (isMusicEnabled) Res.string.music_disable else Res.string.music_enable),
                        onPointerEnter = playHoverSoundEffect,
                    )
                    isInFullscreenMode?.let {
                        AnnoyedPenguinsButton(
                            onButtonPressed = onFullscreenModeToggled,
                            icon = if (isInFullscreenMode) Res.drawable.ic_fullscreen_exit else Res.drawable.ic_fullscreen_enter,
                            title = stringResource(if (isInFullscreenMode) Res.string.fullscreen_exit else Res.string.fullscreen_enter),
                            onPointerEnter = playHoverSoundEffect,
                        )
                    }
                }
                if (shouldShowLogo) {
                    Image(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .weight(1f),
                        painter = painterResource(Res.drawable.img_logo),
                        contentDescription = null,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .weight(1f),
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .background(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                .border(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    width = 2.dp,
                                )
                                .clip(CircleShape)
                                .horizontalScroll(levelSelectorScrollState)
                                .padding(
                                    vertical = 8.dp,
                                    horizontal = 16.dp,
                                ),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            allLevels.forEach { level ->
                                AnnoyedPenguinsButton(
                                    onButtonPressed = { onLevelSelected(level) },
                                    title = if (currentLevel == level) stringResource(Res.string.resume) else level,
                                    onPointerEnter = playHoverSoundEffect,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    AnimatedVisibility(
        visible = isCloseConfirmationDialogVisible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.75f))
                .pointerInput(Unit) {},
        )
    }
    AnimatedVisibility(
        visible = isCloseConfirmationDialogVisible,
        enter = slideIn { IntOffset(0, -it.height) },
        exit = slideOut { IntOffset(0, -it.height) },
    ) {
        CloseConfirmationDialog(
            modifier = modifier,
            onCloseConfirmed = onCloseConfirmed,
            onCloseCancelled = onCloseButtonPressed,
            onPointerEnter = playHoverSoundEffect,
        )
    }
}