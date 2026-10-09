/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testAudio.implementation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.uiComponents.LoadingIndicator
import com.pandulapeter.kubriko.uiComponents.Panel
import kubriko.examples.test_audio.generated.resources.Res
import kubriko.examples.test_audio.generated.resources.ic_loop_off
import kubriko.examples.test_audio.generated.resources.ic_loop_on
import kubriko.examples.test_audio.generated.resources.ic_pause
import kubriko.examples.test_audio.generated.resources.ic_play
import kubriko.examples.test_audio.generated.resources.ic_stop
import kubriko.examples.test_audio.generated.resources.loop_off
import kubriko.examples.test_audio.generated.resources.loop_on
import kubriko.examples.test_audio.generated.resources.pause
import kubriko.examples.test_audio.generated.resources.play
import kubriko.examples.test_audio.generated.resources.stop
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MusicControls(
    title: String,
    isLoaded: Boolean,
    isPlaying: Boolean,
    isLooping: Boolean,
    onPlayPauseClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onLoopClicked: () -> Unit,
) = Panel {
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            text = title,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isLoaded) {
                ControlButton(
                    icon = if (isPlaying) Res.drawable.ic_pause else Res.drawable.ic_play,
                    contentDescription = if (isPlaying) Res.string.pause else Res.string.play,
                    onClick = onPlayPauseClicked,
                )
            } else {
                LoadingIndicator()
            }
            ControlButton(
                icon = Res.drawable.ic_stop,
                contentDescription = Res.string.stop,
                isEnabled = isPlaying,
                onClick = onStopClicked,
            )
            // Looping is applied when playback starts, so it can only be toggled while the track is not playing.
            ControlButton(
                icon = if (isLooping) Res.drawable.ic_loop_on else Res.drawable.ic_loop_off,
                contentDescription = if (isLooping) Res.string.loop_on else Res.string.loop_off,
                isEnabled = !isPlaying,
                onClick = onLoopClicked,
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: DrawableResource,
    contentDescription: StringResource,
    isEnabled: Boolean = true,
    onClick: () -> Unit,
) = Image(
    modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .clickable(enabled = isEnabled, onClick = onClick)
        .alpha(if (isEnabled) 1f else 0.5f),
    colorFilter = ColorFilter.tint(LocalContentColor.current),
    painter = painterResource(icon),
    contentDescription = stringResource(contentDescription),
)
