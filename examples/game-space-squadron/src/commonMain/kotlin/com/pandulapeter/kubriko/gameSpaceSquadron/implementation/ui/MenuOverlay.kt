/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kubriko.examples.game_space_squadron.generated.resources.Res
import kubriko.examples.game_space_squadron.generated.resources.back
import kubriko.examples.game_space_squadron.generated.resources.close_confirmation_positive
import kubriko.examples.game_space_squadron.generated.resources.fullscreen_enter
import kubriko.examples.game_space_squadron.generated.resources.fullscreen_exit
import kubriko.examples.game_space_squadron.generated.resources.ic_back
import kubriko.examples.game_space_squadron.generated.resources.ic_exit
import kubriko.examples.game_space_squadron.generated.resources.ic_fullscreen_enter
import kubriko.examples.game_space_squadron.generated.resources.ic_fullscreen_exit
import kubriko.examples.game_space_squadron.generated.resources.ic_information
import kubriko.examples.game_space_squadron.generated.resources.ic_music_off
import kubriko.examples.game_space_squadron.generated.resources.ic_music_on
import kubriko.examples.game_space_squadron.generated.resources.ic_pause
import kubriko.examples.game_space_squadron.generated.resources.ic_play
import kubriko.examples.game_space_squadron.generated.resources.ic_sound_effects_off
import kubriko.examples.game_space_squadron.generated.resources.ic_sound_effects_on
import kubriko.examples.game_space_squadron.generated.resources.img_logo
import kubriko.examples.game_space_squadron.generated.resources.information
import kubriko.examples.game_space_squadron.generated.resources.music_disable
import kubriko.examples.game_space_squadron.generated.resources.music_enable
import kubriko.examples.game_space_squadron.generated.resources.pause
import kubriko.examples.game_space_squadron.generated.resources.play
import kubriko.examples.game_space_squadron.generated.resources.sound_effects_disable
import kubriko.examples.game_space_squadron.generated.resources.sound_effects_enable
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun SpaceSquadronMenuOverlay(
    modifier: Modifier,
    isVisible: Boolean,
    shouldShowInfoText: Boolean,
    shouldCloseConfirmationDialog: Boolean,
    onPlayButtonPressed: () -> Unit,
    onLeaveButtonPressed: () -> Unit,
    onCloseConfirmed: () -> Unit,
    onPauseButtonPressed: () -> Unit,
    onInfoButtonPressed: () -> Unit,
    areSoundEffectsEnabled: Boolean,
    onSoundEffectsToggled: () -> Unit,
    isMusicEnabled: Boolean,
    onMusicToggled: () -> Unit,
    isInFullscreenMode: Boolean?,
    onFullscreenModeToggled: () -> Unit,
    onButtonHover: () -> Unit,
) = BoxWithConstraints(
    modifier = modifier,
) {
    val shouldShowLogoVertically = maxHeight > 192.dp
    AnimatedVisibility(
        modifier = Modifier.padding(16.dp),
        visible = !isVisible && !shouldShowInfoText && !shouldCloseConfirmationDialog,
        enter = fadeIn() + scaleIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        SpaceSquadronButton(
            onButtonPressed = onPauseButtonPressed,
            icon = Res.drawable.ic_pause,
            title = Res.string.pause,
            onPointerEnter = onButtonHover,
        )
    }
    AnimatedVisibility(
        modifier = Modifier.padding(16.dp),
        visible = shouldShowInfoText,
        enter = fadeIn() + scaleIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        SpaceSquadronButton(
            onButtonPressed = onInfoButtonPressed,
            icon = Res.drawable.ic_back,
            title = Res.string.back,
            onPointerEnter = onButtonHover,
        )
    }
    AnimatedVisibility(
        modifier = Modifier.padding(16.dp),
        visible = shouldShowInfoText,
        enter = fadeIn() + scaleIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        InfoDialog()
    }
    AnimatedVisibility(
        modifier = Modifier.padding(16.dp),
        visible = shouldCloseConfirmationDialog,
        enter = fadeIn() + scaleIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        CloseConfirmationDialog(
            onCloseConfirmed = onCloseConfirmed,
            onCloseCanceled = onLeaveButtonPressed,
            onButtonHover = onButtonHover,
        )
    }
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            visible = isVisible && !shouldShowInfoText && !shouldCloseConfirmationDialog,
            enter = fadeIn() + slideIn { IntOffset(0, -it.height) },
            exit = slideOut { IntOffset(0, -it.height) } + fadeOut(),
        ) {
            Title(
                shouldShowLogoVertically = shouldShowLogoVertically,
                onPlayButtonPressed = onPlayButtonPressed,
                onLeaveButtonPressed = onLeaveButtonPressed,
                onButtonHover = onButtonHover,
            )
        }
        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.End)
                .padding(vertical = 16.dp),
            visible = isVisible && !shouldShowInfoText && !shouldCloseConfirmationDialog,
            enter = fadeIn() + slideIn { IntOffset(0, it.height * 8) },
            exit = slideOut { IntOffset(0, it.height * 8) } + fadeOut(),
        ) {
            UserPreferenceControls(
                onInfoButtonPressed = onInfoButtonPressed,
                areSoundEffectsEnabled = areSoundEffectsEnabled,
                onSoundEffectsToggled = onSoundEffectsToggled,
                isMusicEnabled = isMusicEnabled,
                onMusicToggled = onMusicToggled,
                isInFullscreenMode = isInFullscreenMode,
                onFullscreenModeToggled = onFullscreenModeToggled,
                onButtonHover = onButtonHover,
            )
        }
    }
}

@Composable
private fun Title(
    shouldShowLogoVertically: Boolean,
    onPlayButtonPressed: () -> Unit,
    onLeaveButtonPressed: () -> Unit,
    onButtonHover: () -> Unit,
) = Box(
    modifier = Modifier.fillMaxWidth(),
) {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .heightIn(max = 250.dp)
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(
            modifier = Modifier.weight(1f),
        ) {
            if (shouldShowLogoVertically) {
                Image(
                    modifier = Modifier.align(Alignment.Center),
                    painter = painterResource(Res.drawable.img_logo),
                    contentDescription = null,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpaceSquadronButton(
                onButtonPressed = onPlayButtonPressed,
                icon = Res.drawable.ic_play,
                title = Res.string.play,
                shouldShowTitle = true,
                onPointerEnter = onButtonHover,
            )
            SpaceSquadronButton(
                onButtonPressed = onLeaveButtonPressed,
                icon = Res.drawable.ic_exit,
                title = Res.string.close_confirmation_positive,
                shouldShowTitle = true,
                onPointerEnter = onButtonHover,
            )
        }
    }
}

@Composable
private fun UserPreferenceControls(
    onInfoButtonPressed: () -> Unit,
    areSoundEffectsEnabled: Boolean,
    onSoundEffectsToggled: () -> Unit,
    isMusicEnabled: Boolean,
    onMusicToggled: () -> Unit,
    isInFullscreenMode: Boolean?,
    onFullscreenModeToggled: () -> Unit,
    onButtonHover: () -> Unit,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    SpaceSquadronButton(
        onButtonPressed = onInfoButtonPressed,
        icon = Res.drawable.ic_information,
        title = Res.string.information,
        onPointerEnter = onButtonHover,
    )
    SpaceSquadronButton(
        onButtonPressed = onSoundEffectsToggled,
        icon = if (areSoundEffectsEnabled) Res.drawable.ic_sound_effects_on else Res.drawable.ic_sound_effects_off,
        title = if (areSoundEffectsEnabled) Res.string.sound_effects_disable else Res.string.sound_effects_enable,
        onPointerEnter = onButtonHover,
    )
    SpaceSquadronButton(
        onButtonPressed = onMusicToggled,
        icon = if (isMusicEnabled) Res.drawable.ic_music_on else Res.drawable.ic_music_off,
        title = if (isMusicEnabled) Res.string.music_disable else Res.string.music_enable,
        onPointerEnter = onButtonHover,
    )
    isInFullscreenMode?.let {
        SpaceSquadronButton(
            onButtonPressed = onFullscreenModeToggled,
            icon = if (isInFullscreenMode) Res.drawable.ic_fullscreen_exit else Res.drawable.ic_fullscreen_enter,
            title = if (isInFullscreenMode) Res.string.fullscreen_exit else Res.string.fullscreen_enter,
            onPointerEnter = onButtonHover,
        )
    }
}