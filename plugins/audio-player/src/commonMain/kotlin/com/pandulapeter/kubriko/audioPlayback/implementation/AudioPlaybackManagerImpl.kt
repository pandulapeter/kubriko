package com.pandulapeter.kubriko.audioPlayback.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.AudioPlaybackManager
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class AudioPlaybackManagerImpl : AudioPlaybackManager() {

    private lateinit var audioPlayer: AudioPlayer

    override fun onInitialize(kubriko: Kubriko) {
        audioPlayer = createAudioPlayer(scope)
        kubriko.require<StateManager>().isFocused
            .onEach { isFocused ->
                if (isFocused) {
                    resumeMusic()
                } else {
                    pauseMusic()
                }
            }
            .launchIn(scope)
    }

    override fun onDispose() = audioPlayer.dispose()

    override fun playMusic(uri: String, shouldLoop: Boolean) = audioPlayer.playMusic(uri, shouldLoop)

    override fun resumeMusic() = audioPlayer.resumeMusic()

    override fun pauseMusic() = audioPlayer.pauseMusic()

    override fun stopMusic() = audioPlayer.stopMusic()

    override fun preloadSound(vararg uri: String) = preloadSound(uri.toList())

    override fun preloadSound(uris: List<String>) = audioPlayer.preloadSounds(uris)

    override fun playSound(uri: String) = audioPlayer.playSound(uri)

    override fun unloadSound(vararg uri: String) = unloadSound(uri.toList())

    override fun unloadSound(uris: List<String>) = audioPlayer.unloadSounds(uris)
}