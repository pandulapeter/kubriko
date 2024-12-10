package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.audioPlayback.AudioPlaybackManager

internal class AudioPlaybackManagerImpl : AudioPlaybackManager() {

    private lateinit var audioPlayer: AudioPlayer

    @Composable
    override fun onRecomposition() {
        audioPlayer = rememberAudioPlayer()
    }

    override fun onDispose() = audioPlayer.dispose()

    override fun playSound(uri: String) = audioPlayer.playSound(uri)
}