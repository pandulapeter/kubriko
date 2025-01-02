package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope

internal interface AudioPlayer {

    fun playMusic(uri: String, shouldLoop: Boolean)

    fun resumeMusic()

    fun pauseMusic()

    fun stopMusic()

    fun playSound(uri: String)

    fun dispose()
}

@Composable
internal expect fun createAudioPlayer(coroutineScope: CoroutineScope): AudioPlayer