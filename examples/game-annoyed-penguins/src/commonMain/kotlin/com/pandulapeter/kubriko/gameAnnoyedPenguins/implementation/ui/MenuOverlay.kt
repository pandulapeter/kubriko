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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kubriko.examples.game_annoyed_penguins.generated.resources.Res
import kubriko.examples.game_annoyed_penguins.generated.resources.back
import kubriko.examples.game_annoyed_penguins.generated.resources.fullscreen_enter
import kubriko.examples.game_annoyed_penguins.generated.resources.fullscreen_exit
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_back
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_fullscreen_enter
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_fullscreen_exit
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_information
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_music_off
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_music_on
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_play
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_sound_effects_off
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_sound_effects_on
import kubriko.examples.game_annoyed_penguins.generated.resources.img_logo
import kubriko.examples.game_annoyed_penguins.generated.resources.information
import kubriko.examples.game_annoyed_penguins.generated.resources.information_contents
import kubriko.examples.game_annoyed_penguins.generated.resources.level
import kubriko.examples.game_annoyed_penguins.generated.resources.music_disable
import kubriko.examples.game_annoyed_penguins.generated.resources.music_enable
import kubriko.examples.game_annoyed_penguins.generated.resources.sound_effects_disable
import kubriko.examples.game_annoyed_penguins.generated.resources.sound_effects_enable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MenuOverlay(
    modifier: Modifier = Modifier,
    onInfoButtonPressed: () -> Unit,
    areSoundEffectsEnabled: Boolean,
    onSoundEffectsToggled: () -> Unit,
    isMusicEnabled: Boolean,
    onMusicToggled: () -> Unit,
    isInFullscreenMode: Boolean?,
    onFullscreenModeToggled: () -> Unit,
    onPointerEnter: () -> Unit = {},
    shouldUseLandscapeLayout: Boolean,
    isInfoDialogVisible: Boolean,
    levelSelectorScrollState: ScrollState = rememberScrollState(),
) {
    AnimatedVisibility(
        visible = isInfoDialogVisible,
        enter = slideIn { IntOffset(0, it.height) },
        exit = slideOut { IntOffset(0, it.height) },
    ) {
        InfoDialog(
            modifier = modifier,
            onInfoButtonPressed = onInfoButtonPressed,
            onPointerEnter = onPointerEnter,
        )
    }
    AnimatedVisibility(
        visible = !isInfoDialogVisible,
        enter = slideIn { IntOffset(0, -it.height) },
        exit = slideOut { IntOffset(0, -it.height) },
    ) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
                    .weight(0.75f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AnnoyedPenguinsButton(
                    icon = Res.drawable.ic_information,
                    title = stringResource(Res.string.information),
                    onButtonPressed = onInfoButtonPressed,
                    onPointerEnter = onPointerEnter,
                )
                AnnoyedPenguinsButton(
                    onButtonPressed = onSoundEffectsToggled,
                    icon = if (areSoundEffectsEnabled) Res.drawable.ic_sound_effects_on else Res.drawable.ic_sound_effects_off,
                    title = stringResource(if (areSoundEffectsEnabled) Res.string.sound_effects_disable else Res.string.sound_effects_enable),
                    onPointerEnter = onPointerEnter,
                )
                AnnoyedPenguinsButton(
                    onButtonPressed = onMusicToggled,
                    icon = if (isMusicEnabled) Res.drawable.ic_music_on else Res.drawable.ic_music_off,
                    title = stringResource(if (isMusicEnabled) Res.string.music_disable else Res.string.music_enable),
                    onPointerEnter = onPointerEnter,
                )
                isInFullscreenMode?.let {
                    AnnoyedPenguinsButton(
                        onButtonPressed = onFullscreenModeToggled,
                        icon = if (isInFullscreenMode) Res.drawable.ic_fullscreen_exit else Res.drawable.ic_fullscreen_enter,
                        title = stringResource(if (isInFullscreenMode) Res.string.fullscreen_exit else Res.string.fullscreen_enter),
                        onPointerEnter = onPointerEnter,
                    )
                }
            }
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 8.dp,
                        horizontal = 16.dp,
                    )
                    .weight(2.5f),
                painter = painterResource(Res.drawable.img_logo),
                contentDescription = null,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (shouldUseLandscapeLayout) 1f else 2.25f),
            ) {
                AnimatedContent(
                    targetState = shouldUseLandscapeLayout,
                ) { shouldUseLandscapeLayout ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            @Composable
                            fun levelItems() = (1..3).toList().forEach { index ->
                                AnnoyedPenguinsButton(
                                    onButtonPressed = {},
                                    icon = Res.drawable.ic_play,
                                    shouldShowTitle = true,
                                    title = stringResource(Res.string.level, index),
                                    onPointerEnter = onPointerEnter,
                                )
                            }
                            if (shouldUseLandscapeLayout) {
                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(levelSelectorScrollState)
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    levelItems()
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .verticalScroll(levelSelectorScrollState)
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    levelItems()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoDialog(
    modifier: Modifier = Modifier,
    onInfoButtonPressed: () -> Unit,
    onPointerEnter: () -> Unit = {},
) = Box(
    modifier = modifier.fillMaxSize(),
) {
    Text(
        modifier = Modifier
            .align(Alignment.Center)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 56.dp),
        color = MaterialTheme.colorScheme.primary,
        text = stringResource(Res.string.information_contents),
    )
    AnnoyedPenguinsButton(
        modifier = Modifier
            .padding(16.dp)
            .align(Alignment.BottomEnd),
        icon = Res.drawable.ic_back,
        title = stringResource(Res.string.back),
        onButtonPressed = onInfoButtonPressed,
        onPointerEnter = onPointerEnter,
    )
}