/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameBlockysJourney.implementation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kubriko.examples.game_blockys_journey.generated.resources.Res
import kubriko.examples.game_blockys_journey.generated.resources.close_confirmation_positive
import kubriko.examples.game_blockys_journey.generated.resources.fullscreen_enter
import kubriko.examples.game_blockys_journey.generated.resources.fullscreen_exit
import kubriko.examples.game_blockys_journey.generated.resources.ic_exit
import kubriko.examples.game_blockys_journey.generated.resources.ic_fullscreen_enter
import kubriko.examples.game_blockys_journey.generated.resources.ic_fullscreen_exit
import kubriko.examples.game_blockys_journey.generated.resources.ic_information
import kubriko.examples.game_blockys_journey.generated.resources.ic_music_off
import kubriko.examples.game_blockys_journey.generated.resources.ic_music_on
import kubriko.examples.game_blockys_journey.generated.resources.ic_sound_effects_off
import kubriko.examples.game_blockys_journey.generated.resources.ic_sound_effects_on
import kubriko.examples.game_blockys_journey.generated.resources.img_logo
import kubriko.examples.game_blockys_journey.generated.resources.information
import kubriko.examples.game_blockys_journey.generated.resources.music_disable
import kubriko.examples.game_blockys_journey.generated.resources.music_enable
import kubriko.examples.game_blockys_journey.generated.resources.resume
import kubriko.examples.game_blockys_journey.generated.resources.sound_effects_disable
import kubriko.examples.game_blockys_journey.generated.resources.sound_effects_enable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MenuOverlay(
    windowInsets: WindowInsets,
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
    onPlayButtonPressed: () -> Unit,
    isSceneEditorEnabled: Boolean,
    levelSelectorScrollState: ScrollState = rememberScrollState(),
) = BoxWithConstraints {
    val shouldShowLogoVertically = maxHeight > 192.dp
    AnimatedVisibility(
        visible = isInfoDialogVisible,
        enter = slideIn { IntOffset(0, -it.height) },
        exit = slideOut { IntOffset(0, -it.height) },
    ) {
        InfoDialog(
            onInfoButtonPressed = onInfoButtonPressed,
            onPointerEnter = playHoverSoundEffect,
            windowInsets = windowInsets,
        )
    }
    AnimatedVisibility(
        visible = !isInfoDialogVisible,
        enter = slideIn { IntOffset(0, it.height) },
        exit = slideOut { IntOffset(0, it.height) },
    ) {
        BoxWithConstraints(
            modifier = Modifier.windowInsetsPadding(windowInsets),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val bias by animateFloatAsState(if (shouldShowLogoVertically) 2.5f else 0.75f)
                Box(
                    modifier = Modifier.weight(bias),
                ) {
                    if (shouldShowLogoVertically) {
                        Image(
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(0.9f)
                                .padding(top = 56.dp),
                            alignment = BiasAlignment(0f, 0.5f),
                            painter = painterResource(Res.drawable.img_logo),
                            contentScale = ContentScale.Inside,
                            contentDescription = null,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            BlockysJourneyButton(
                                onButtonPressed = onCloseButtonPressed,
                                icon = Res.drawable.ic_exit,
                                title = stringResource(Res.string.close_confirmation_positive),
                                onPointerEnter = playHoverSoundEffect,
                            )
                            BlockysJourneyButton(
                                icon = Res.drawable.ic_information,
                                title = stringResource(Res.string.information),
                                onButtonPressed = onInfoButtonPressed,
                                onPointerEnter = playHoverSoundEffect,
                            )
                            if (isSceneEditorEnabled) {
                                PlatformSpecificContent(
                                    playHoverSoundEffect = playHoverSoundEffect,
                                    playToggleSoundEffect = playToggleSoundEffect,
                                )
                            }
                        }
                        Spacer(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 6.dp)
                                .weight(1f),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            BlockysJourneyButton(
                                onButtonPressed = onSoundEffectsToggled,
                                icon = if (areSoundEffectsEnabled) Res.drawable.ic_sound_effects_on else Res.drawable.ic_sound_effects_off,
                                title = stringResource(if (areSoundEffectsEnabled) Res.string.sound_effects_disable else Res.string.sound_effects_enable),
                                onPointerEnter = playHoverSoundEffect,
                            )
                            BlockysJourneyButton(
                                onButtonPressed = onMusicToggled,
                                icon = if (isMusicEnabled) Res.drawable.ic_music_on else Res.drawable.ic_music_off,
                                title = stringResource(if (isMusicEnabled) Res.string.music_disable else Res.string.music_enable),
                                onPointerEnter = playHoverSoundEffect,
                            )
                            isInFullscreenMode?.let {
                                BlockysJourneyButton(
                                    onButtonPressed = onFullscreenModeToggled,
                                    icon = if (isInFullscreenMode) Res.drawable.ic_fullscreen_exit else Res.drawable.ic_fullscreen_enter,
                                    title = stringResource(if (isInFullscreenMode) Res.string.fullscreen_exit else Res.string.fullscreen_enter),
                                    onPointerEnter = playHoverSoundEffect,
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BlockysJourneyButton(
                                onButtonPressed = onPlayButtonPressed,
                                title = stringResource(Res.string.resume),
                                onPointerEnter = playHoverSoundEffect,
                            )
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
            modifier = Modifier.windowInsetsPadding(windowInsets),
            onCloseConfirmed = onCloseConfirmed,
            onCloseCancelled = onCloseButtonPressed,
            onPointerEnter = playHoverSoundEffect,
        )
    }
}