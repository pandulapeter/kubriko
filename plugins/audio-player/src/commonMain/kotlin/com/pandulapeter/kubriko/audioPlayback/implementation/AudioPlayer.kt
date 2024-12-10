package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable

internal interface AudioPlayer {

    fun playSound(uri: String)

    fun dispose()
}

@Composable
internal expect fun rememberAudioPlayer(): AudioPlayer