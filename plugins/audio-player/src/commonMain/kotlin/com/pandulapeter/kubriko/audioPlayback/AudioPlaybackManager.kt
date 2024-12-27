package com.pandulapeter.kubriko.audioPlayback

import com.pandulapeter.kubriko.manager.Manager

/**
 * TODO: Documentation
 */
sealed class AudioPlaybackManager : Manager() {

    // TODO: Add ability to preload sounds
    // TODO: Add ability to play multiple music tracks

    abstract fun playMusic(uri: String, shouldLoop: Boolean)

    abstract fun resumeMusic()

    abstract fun pauseMusic()

    abstract fun stopMusic()

    abstract fun playSound(uri: String)

    companion object {
        fun newInstance(): AudioPlaybackManager = AudioPlaybackManagerImpl()
    }
}