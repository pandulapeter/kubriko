package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun rememberAudioPlayer() = remember {
    object : AudioPlayer {

        override fun play(uri: String) {
            AVAudioPlayer(NSURL.URLWithString(URLString = uri)!!, error = null).run {
                prepareToPlay()
                play()
            }
        }

        override fun dispose() = Unit
    }
}