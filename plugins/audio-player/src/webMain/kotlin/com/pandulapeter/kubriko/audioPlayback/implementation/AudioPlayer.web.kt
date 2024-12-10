package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.document
import org.w3c.dom.HTMLAudioElement
import org.w3c.dom.events.Event

@Composable
internal actual fun rememberAudioPlayer() = remember {
    object : AudioPlayer {

        override fun playSound(uri: String) {
            val audioElement = document.createElement("audio") as HTMLAudioElement
            audioElement.src = uri
            audioElement.play()
            val onEnded: (Event) -> Unit = {
                audioElement.pause()
                audioElement.src = ""
                audioElement.remove()
            }
            audioElement.addEventListener("ended", onEnded)
        }

        override fun dispose() = Unit
    }
}