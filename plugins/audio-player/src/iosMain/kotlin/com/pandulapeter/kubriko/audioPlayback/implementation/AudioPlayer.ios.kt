package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberAudioPlayer() = remember {
    object : AudioPlayer {
        // TODO: iOS audio player
        override fun play(uri: String) = println("[iOS] play $uri")

        override fun dispose() = Unit
    }
}