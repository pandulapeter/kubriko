package com.pandulapeter.kubriko.audioPlayback.implementation

import kotlinx.coroutines.CoroutineScope

internal interface AudioPlayer {

    fun playMusic(uri: String, shouldLoop: Boolean)

    fun resumeMusic()

    fun pauseMusic()

    fun stopMusic()

    fun preloadSounds(uris: Collection<String>)

    fun playSound(uri: String)

    fun unloadSounds(uris: Collection<String>)

    fun dispose()
}

internal expect fun createAudioPlayer(coroutineScope: CoroutineScope): AudioPlayer