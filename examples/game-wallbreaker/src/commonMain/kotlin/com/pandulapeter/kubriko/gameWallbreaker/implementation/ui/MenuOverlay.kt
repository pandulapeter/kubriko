/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameWallbreaker.implementation.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kubriko.examples.game_wallbreaker.generated.resources.Res
import kubriko.examples.game_wallbreaker.generated.resources.close_confirmation_positive
import kubriko.examples.game_wallbreaker.generated.resources.fullscreen_enter
import kubriko.examples.game_wallbreaker.generated.resources.fullscreen_exit
import kubriko.examples.game_wallbreaker.generated.resources.ic_exit
import kubriko.examples.game_wallbreaker.generated.resources.ic_fullscreen_enter
import kubriko.examples.game_wallbreaker.generated.resources.ic_fullscreen_exit
import kubriko.examples.game_wallbreaker.generated.resources.ic_information
import kubriko.examples.game_wallbreaker.generated.resources.ic_music_off
import kubriko.examples.game_wallbreaker.generated.resources.ic_music_on
import kubriko.examples.game_wallbreaker.generated.resources.ic_play
import kubriko.examples.game_wallbreaker.generated.resources.ic_sound_effects_off
import kubriko.examples.game_wallbreaker.generated.resources.ic_sound_effects_on
import kubriko.examples.game_wallbreaker.generated.resources.img_logo
import kubriko.examples.game_wallbreaker.generated.resources.information
import kubriko.examples.game_wallbreaker.generated.resources.music_disable
import kubriko.examples.game_wallbreaker.generated.resources.music_enable
import kubriko.examples.game_wallbreaker.generated.resources.play
import kubriko.examples.game_wallbreaker.generated.resources.sound_effects_disable
import kubriko.examples.game_wallbreaker.generated.resources.sound_effects_enable
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun MenuOverlay(
    modifier: Modifier,
    isVisible: Boolean,
    isActive: Boolean,
    shouldShowResumeButton: Boolean,
    onResumeButtonPressed: () -> Unit,
    onRestartButtonPressed: () -> Unit,
    onInfoButtonPressed: () -> Unit,
    onExitButtonPressed: () -> Unit,
    areSoundEffectsEnabled: Boolean,
    onSoundEffectsToggled: () -> Unit,
    isMusicEnabled: Boolean,
    onMusicToggled: () -> Unit,
    isInFullscreenMode: Boolean?,
    onFullscreenModeToggled: () -> Unit,
    onButtonHover: () -> Unit,
) = WallbreakerAnimatedVisibility(
    modifier = modifier,
    visible = isVisible,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                alignment = Alignment.BottomCenter,
                painter = painterResource(Res.drawable.img_logo),
                contentDescription = null,
            )
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 8.dp),
            ) {
                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = if (maxHeight < 128.dp) 2 else 1,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.95f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        WallbreakerButton(
                            modifier = if (isActive) Modifier.scale(scale) else Modifier,
                            onButtonPressed = if (shouldShowResumeButton) onResumeButtonPressed else onRestartButtonPressed,
                            icon = Res.drawable.ic_play,
                            contentDescription = Res.string.play,
                            containerColor = createButtonColor(0.3f),
                            onPointerEnter = onButtonHover,
                        )
                        WallbreakerButton(
                            onButtonPressed = onInfoButtonPressed,
                            icon = Res.drawable.ic_information,
                            contentDescription = Res.string.information,
                            containerColor = createButtonColor(0.45f),
                            onPointerEnter = onButtonHover,
                        )
                        WallbreakerButton(
                            onButtonPressed = onExitButtonPressed,
                            icon = Res.drawable.ic_exit,
                            contentDescription = Res.string.close_confirmation_positive,
                            containerColor = createButtonColor(0.6f),
                            onPointerEnter = onButtonHover,
                        )
                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WallbreakerButton(
                            onButtonPressed = onSoundEffectsToggled,
                            icon = if (areSoundEffectsEnabled) Res.drawable.ic_sound_effects_on else Res.drawable.ic_sound_effects_off,
                            contentDescription = if (areSoundEffectsEnabled) Res.string.sound_effects_disable else Res.string.sound_effects_enable,
                            containerColor = createButtonColor(0.75f),
                            onPointerEnter = onButtonHover,
                        )
                        WallbreakerButton(
                            onButtonPressed = onMusicToggled,
                            icon = if (isMusicEnabled) Res.drawable.ic_music_on else Res.drawable.ic_music_off,
                            contentDescription = if (isMusicEnabled) Res.string.music_disable else Res.string.music_enable,
                            containerColor = createButtonColor(0.9f),
                            onPointerEnter = onButtonHover,
                        )
                        isInFullscreenMode?.let {
                            WallbreakerButton(
                                onButtonPressed = onFullscreenModeToggled,
                                icon = if (isInFullscreenMode) Res.drawable.ic_fullscreen_exit else Res.drawable.ic_fullscreen_enter,
                                contentDescription = if (isInFullscreenMode) Res.string.fullscreen_exit else Res.string.fullscreen_enter,
                                containerColor = createButtonColor(0.05f),
                                onPointerEnter = onButtonHover,
                            )
                        }
                    }
                }
            }
        }
    }
}