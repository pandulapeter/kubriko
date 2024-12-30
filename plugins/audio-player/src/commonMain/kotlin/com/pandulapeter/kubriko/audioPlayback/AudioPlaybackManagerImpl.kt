package com.pandulapeter.kubriko.audioPlayback

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.implementation.createAudioPlayer
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class AudioPlaybackManagerImpl : AudioPlaybackManager() {

    private val audioPlayer by autoInitializingLazy { createAudioPlayer(scope) }
    private var musicUri: String? = null
    private var shouldLoopMusic = false

    override fun onInitialize(kubriko: Kubriko) {
        val stateManager = kubriko.get<StateManager>()
        musicUri?.let {
            if (stateManager.isFocused.value) {
                audioPlayer.playMusic(it, shouldLoopMusic)
            }
            musicUri = null
        }
        stateManager.isFocused
            .onEach { isFocused ->
                if (!isFocused) {
                    pauseMusic()
                }
            }
            .launchIn(scope)
    }

    override fun onDispose() = audioPlayer.dispose()

    override fun playMusic(uri: String, shouldLoop: Boolean) {
        if (isInitialized.value) {
            audioPlayer.playMusic(uri, shouldLoop)
        } else {
            musicUri = uri
            shouldLoopMusic = shouldLoop
        }
    }

    override fun resumeMusic() = audioPlayer.resumeMusic()

    override fun pauseMusic() = audioPlayer.pauseMusic()

    override fun stopMusic() {
        if (isInitialized.value) {
            audioPlayer.stopMusic()
        }
        musicUri = null
    }

    override fun playSound(uri: String) {
        if (isInitialized.value) {
            audioPlayer.playSound(uri)
        }
    }
}