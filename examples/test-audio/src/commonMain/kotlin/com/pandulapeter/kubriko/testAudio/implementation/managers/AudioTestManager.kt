/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.testAudio.implementation.managers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.testAudio.implementation.ui.MusicControls
import com.pandulapeter.kubriko.testAudio.implementation.utilities.getResourceUri
import com.pandulapeter.kubriko.uiComponents.InfoPanel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kubriko.examples.test_audio.generated.resources.Res
import kubriko.examples.test_audio.generated.resources.description
import kubriko.examples.test_audio.generated.resources.music_track_1
import kubriko.examples.test_audio.generated.resources.music_track_2
import org.jetbrains.compose.resources.stringResource

internal class AudioTestManager(
    webRootPathName: String,
) : Manager() {
    private val musicManager by manager<MusicManager>()
    private val stateManager by manager<StateManager>()
    private val isTrack1Playing = mutableStateOf(false)
    private val isTrack2Playing = mutableStateOf(false)
    private val track1Uri = getResourceUri(URI_MUSIC_1, webRootPathName)
    private val track2Uri = getResourceUri(URI_MUSIC_2, webRootPathName)
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

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        isTrack1Playing.value = musicManager.isPlaying(track1Uri)
        isTrack2Playing.value = musicManager.isPlaying(track2Uri)
    }

    fun stopMusicBeforeDispose() = shouldStopMusic.update { true }

    private fun togglePlayback(uri: String, isPlaying: Boolean) = if (isPlaying) musicManager.pause(uri) else musicManager.play(uri)

    @Composable
    override fun Composable(windowInsets: WindowInsets) = Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(windowInsets)
            .padding(16.dp),
    ) {
        InfoPanel(
            stringResource = Res.string.description,
            isVisible = StateHolder.isInfoPanelVisible.value,
        )
        MusicControls(
            title = stringResource(Res.string.music_track_1),
            isLoaded = remember { musicManager.getLoadingProgress(track1Uri) }.collectAsState(0f).value == 1f,
            isPlaying = isTrack1Playing.value,
            onPlayPauseClicked = { togglePlayback(track1Uri, isTrack1Playing.value) },
            onStopClicked = { musicManager.stop(track1Uri) },
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        MusicControls(
            title = stringResource(Res.string.music_track_2),
            isLoaded = remember { musicManager.getLoadingProgress(track2Uri) }.collectAsState(0f).value == 1f,
            isPlaying = isTrack2Playing.value,
            onPlayPauseClicked = { togglePlayback(track2Uri, isTrack2Playing.value) },
            onStopClicked = { musicManager.stop(track2Uri) },
        )
    }

    companion object {
        private const val URI_MUSIC_1 = "files/music/a_csajod-atkok.mp3"
        private const val URI_MUSIC_2 = "files/music/a_csajod-energiavampir.mp3"
    }
}
