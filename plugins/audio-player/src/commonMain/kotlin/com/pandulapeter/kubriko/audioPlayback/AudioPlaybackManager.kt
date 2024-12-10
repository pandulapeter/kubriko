package com.pandulapeter.kubriko.audioPlayback

import com.pandulapeter.kubriko.audioPlayback.implementation.AudioPlaybackManagerImpl
import com.pandulapeter.kubriko.manager.Manager

/**
 * TODO: Documentation
 */
abstract class AudioPlaybackManager : Manager() {

    abstract fun preloadSounds(uris: List<String>)

    abstract fun playSound(uri: String)

    abstract fun unloadSound(uri: String)

    // TODO: abstract fun playMusic(uri: String)

    companion object {
        fun newInstance(): AudioPlaybackManager = AudioPlaybackManagerImpl()
    }
}