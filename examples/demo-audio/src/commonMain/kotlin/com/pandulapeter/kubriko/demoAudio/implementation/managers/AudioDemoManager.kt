/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoAudio.implementation.managers

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kubriko.examples.demo_audio.generated.resources.Res
import kubriko.examples.demo_audio.generated.resources.ic_loop_on
import kubriko.examples.demo_audio.generated.resources.ic_pause
import kubriko.examples.demo_audio.generated.resources.ic_play
import kubriko.examples.demo_audio.generated.resources.ic_stop
import kubriko.examples.demo_audio.generated.resources.loop_on
import kubriko.examples.demo_audio.generated.resources.pause
import kubriko.examples.demo_audio.generated.resources.play
import kubriko.examples.demo_audio.generated.resources.stop
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalResourceApi::class)
internal class AudioDemoManager : Manager() {
    private val musicManager by manager<MusicManager>()
    private val stateManager by manager<StateManager>()
    private var isTrack1Playing = mutableStateOf(false)
    private var isTrack2Playing = mutableStateOf(false)
    private val track1Uri = Res.getUri(URI_MUSIC_1)
    private val track2Uri = Res.getUri(URI_MUSIC_2)
    private val shouldStopMusic = MutableStateFlow(false)

    override fun onInitialize(kubriko: Kubriko) {
        musicManager.preload(track1Uri, track2Uri)
        shouldStopMusic
            .filter { it }
            .onEach {
                musicManager.stop(track1Uri)
                musicManager.stop(track2Uri)
            }
            .launchIn(scope)
        stateManager.isFocused
            .filter { it }
            .onEach { shouldStopMusic.update { false } }
            .launchIn(scope)
    }

    override fun onUpdate(deltaTimeInMilliseconds: Float, gameTimeMilliseconds: Long) {
        isTrack1Playing.value = musicManager.isPlaying(track1Uri)
        isTrack2Playing.value = musicManager.isPlaying(track2Uri)
    }

    fun stopMusicBeforeDispose() = shouldStopMusic.update { true }

    @Composable
    override fun Composable(insetPaddingModifier: Modifier) = Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MusicControls(
                title = "A Csajod - Átkok",
                musicUri = track1Uri,
                isPlaying = isTrack1Playing.value,
            )
            MusicControls(
                title = "A Csajod - Energiavámpír",
                musicUri = track2Uri,
                isPlaying = isTrack2Playing.value,
            )
        }
    }

    @Composable
    private fun MusicControls(
        title: String,
        musicUri: String,
        isPlaying: Boolean,
    ) = Card {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = title,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ControlButton(
                    icon = if (isPlaying) Res.drawable.ic_pause else Res.drawable.ic_play,
                    contentDescription = if (isPlaying) Res.string.pause else Res.string.play,
                    onClick = { if (isPlaying) musicManager.pause(musicUri) else musicManager.play(musicUri) },
                )
                ControlButton(
                    icon = Res.drawable.ic_stop,
                    contentDescription = Res.string.stop,
                    isEnabled = isPlaying,
                    onClick = { musicManager.stop(musicUri) },
                )
                ControlButton(
                    icon = Res.drawable.ic_loop_on,
                    contentDescription = Res.string.loop_on,
                    isEnabled = false,
                    onClick = {},
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
            .padding(8.dp)
            .alpha(if (isEnabled) 1f else 0.5f),
        colorFilter = ColorFilter.tint(LocalContentColor.current),
        painter = painterResource(icon),
        contentDescription = stringResource(contentDescription),
    )

    companion object {
        private const val URI_MUSIC_1 = "files/music/a_csajod-átkok.mp3"
        private const val URI_MUSIC_2 = "files/music/a_csajod-energiavámpír.mp3"
    }
}