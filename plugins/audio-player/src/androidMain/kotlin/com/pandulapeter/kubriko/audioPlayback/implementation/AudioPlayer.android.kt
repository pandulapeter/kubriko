package com.pandulapeter.kubriko.audioPlayback.implementation

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberAudioPlayer() = remember {
    object : AudioPlayer {

        override fun play(uri: String) {
            MediaPlayer().run {
                setDataSource(uri)
                setOnPreparedListener { it.start() }
                setOnCompletionListener { it.release() }
                prepareAsync()
            }
        }

        override fun dispose() = Unit
    }
}