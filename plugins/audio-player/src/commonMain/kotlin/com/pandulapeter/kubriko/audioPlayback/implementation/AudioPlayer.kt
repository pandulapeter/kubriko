package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable

internal interface AudioPlayer {

    fun preloadSounds(uris: List<String>)

    fun playSound(uri: String)

    fun unloadSound(uri: String)

    fun dispose()
}

@Composable
internal expect fun rememberAudioPlayer(): AudioPlayer