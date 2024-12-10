package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun rememberAudioPlayer(): AudioPlayer {
    val scope = rememberCoroutineScope()
    return remember {
        object : AudioPlayer {

            override fun play(uri: String) {
                scope.launch(Dispatchers.Default) {
                    AVAudioPlayer(NSURL.URLWithString(URLString = uri)!!, error = null).run {
                        prepareToPlay()
                        play()
                        do {
                            delay(100)
                        } while (isPlaying())
                        dispose()
                    }
                }
            }

            override fun dispose() = Unit
        }
    }
}