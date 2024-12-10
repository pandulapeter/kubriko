package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.audioPlayback.AudioPlaybackManager

internal class AudioPlaybackManagerImpl : AudioPlaybackManager() {

    private lateinit var audioPlayer: AudioPlayer

    // TODO: Initializing the audioPlayer here is too late
    @Composable
    override fun onRecomposition() {
        audioPlayer = rememberAudioPlayer()
    }

    override fun onDispose() = audioPlayer.dispose()

    override fun preloadSounds(uris: List<String>) = audioPlayer.preloadSounds(uris)

    override fun playSound(uri: String) = audioPlayer.playSound(uri)

    override fun unloadSound(uri: String) = audioPlayer.unloadSound(uri)
}