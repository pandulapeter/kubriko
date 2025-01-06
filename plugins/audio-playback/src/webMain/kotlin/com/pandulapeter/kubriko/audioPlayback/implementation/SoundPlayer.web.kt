package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import kotlinx.browser.document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.HTMLAudioElement

@Composable
internal actual fun createSoundPlayer() = object : SoundPlayer {

    override suspend fun preload(uri: String) = withContext(Dispatchers.Default) {
        (document.createElement("audio") as HTMLAudioElement).apply {
            src = uri
        }
    }

    override suspend fun play(sound: Any) {
        withContext(Dispatchers.Default) {
            (sound as HTMLAudioElement).play()
        }
    }

    override suspend fun dispose(sound: Any) {
        (sound as HTMLAudioElement).run {
            if (!paused) {
                pause()
            }
            src = ""
            remove()
        }
    }

    override suspend fun dispose() = Unit
}