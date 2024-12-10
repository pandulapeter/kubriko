package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.browser.document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLAudioElement

@Composable
internal actual fun rememberAudioPlayer(): AudioPlayer {
    val coroutineScope = rememberCoroutineScope()
    return remember {
        object : AudioPlayer {
            private val audioElements = mutableMapOf<String, HTMLAudioElement>()

            private fun preloadSound(uri: String) {
                coroutineScope.launch(Dispatchers.Default) {
                    if (audioElements[uri] == null) {
                        audioElements[uri] = (document.createElement("audio") as HTMLAudioElement).apply {
                            src = uri
                        }
                    }
                }
            }

            override fun preloadSounds(uris: List<String>) = uris.forEach(::preloadSound)

            override fun playSound(uri: String) {
                audioElements[uri].let { audioElement ->
                    if (audioElement == null) {
                        coroutineScope.launch {
                            preloadSound(uri)
                            do {
                                delay(50)
                            } while (audioElements[uri] == null)
                        }
                        playSound(uri)
                    } else {
                        audioElement.play()
                    }
                }
            }

            override fun unloadSound(uri: String) {
                audioElements[uri]?.unload()
                audioElements.remove(uri)
            }

            override fun dispose() {
                audioElements.values.forEach { it.unload() }
                audioElements.clear()
            }

            private fun HTMLAudioElement.unload() {
                pause()
                src = ""
                remove()
            }
        }
    }
}