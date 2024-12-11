package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable

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

@Composable
internal expect fun rememberAudioPlayer(): AudioPlayer