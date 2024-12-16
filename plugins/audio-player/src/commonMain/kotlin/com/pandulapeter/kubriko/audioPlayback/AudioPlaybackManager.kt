package com.pandulapeter.kubriko.audioPlayback

import com.pandulapeter.kubriko.audioPlayback.implementation.AudioPlaybackManagerImpl
import com.pandulapeter.kubriko.manager.Manager

/**
 * TODO: Documentation
 */
abstract class AudioPlaybackManager : Manager() {

    abstract fun playMusic(uri: String, shouldLoop: Boolean)

    abstract fun resumeMusic()

    abstract fun pauseMusic()

    abstract fun stopMusic()

    abstract fun playSound(uri: String)

    companion object {
        fun newInstance(): AudioPlaybackManager = AudioPlaybackManagerImpl()
    }
}