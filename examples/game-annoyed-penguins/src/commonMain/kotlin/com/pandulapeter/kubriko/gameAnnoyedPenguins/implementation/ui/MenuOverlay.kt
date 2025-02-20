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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.uiComponents.SmallButton
import kubriko.examples.game_annoyed_penguins.generated.resources.Res
import kubriko.examples.game_annoyed_penguins.generated.resources.fullscreen_enter
import kubriko.examples.game_annoyed_penguins.generated.resources.fullscreen_exit
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
import kubriko.examples.game_annoyed_penguins.generated.resources.sound_effects_disable
import kubriko.examples.game_annoyed_penguins.generated.resources.sound_effects_enable
import org.jetbrains.compose.resources.painterResource

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
) = Column(
    modifier = modifier
        .fillMaxSize()
        .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Row(
        modifier = Modifier.align(Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SmallButton(
            icon = Res.drawable.ic_information,
            contentDescription = Res.string.information,
            onButtonPressed = onInfoButtonPressed,
        )
        SmallButton(
            onButtonPressed = onSoundEffectsToggled,
            icon = if (areSoundEffectsEnabled) Res.drawable.ic_sound_effects_on else Res.drawable.ic_sound_effects_off,
            contentDescription = if (areSoundEffectsEnabled) Res.string.sound_effects_disable else Res.string.sound_effects_enable,
        )
        SmallButton(
            onButtonPressed = onMusicToggled,
            icon = if (isMusicEnabled) Res.drawable.ic_music_on else Res.drawable.ic_music_off,
            contentDescription = if (isMusicEnabled) Res.string.music_disable else Res.string.music_enable,
        )
        isInFullscreenMode?.let {
            SmallButton(
                onButtonPressed = onFullscreenModeToggled,
                icon = if (isInFullscreenMode) Res.drawable.ic_fullscreen_exit else Res.drawable.ic_fullscreen_enter,
                contentDescription = if (isInFullscreenMode) Res.string.fullscreen_exit else Res.string.fullscreen_enter,
            )
        }
    }
    Image(
        modifier = Modifier.weight(1f).scale(0.75f),
        painter = painterResource(Res.drawable.img_logo),
        contentScale = ContentScale.Inside,
        contentDescription = null,
    )
    Text(
        modifier = Modifier.padding(16.dp),
        text = "Work in progress...",
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
    )
}