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
    val coroutineScope = rememberCoroutineScope()
    return remember {
        object : AudioPlayer {
            private val audioPlayers = mutableMapOf<String, AVAudioPlayer>()

            private fun preloadSound(uri: String) {
                coroutineScope.launch(Dispatchers.Default) {
                    if (audioPlayers[uri] == null) {
                        audioPlayers[uri] = AVAudioPlayer(NSURL.URLWithString(URLString = uri)!!, error = null).apply {
                            prepareToPlay()
                        }
                    }
                }
            }

            override fun preloadSounds(uris: Collection<String>) = uris.forEach(::preloadSound)

            override fun playSound(uri: String) {
                audioPlayers[uri].let { audioPlayer ->
                    if (audioPlayer == null) {
                        coroutineScope.launch {
                            preloadSound(uri)
                            do {
                                delay(50)
                            } while (audioPlayers[uri] == null)
                        }
                        playSound(uri)
                    } else {
                        audioPlayer.play()
                    }
                }
            }

            override fun unloadSounds(uris: Collection<String>) = uris.forEach { uri ->
                audioPlayers[uri]?.unload()
                audioPlayers.remove(uri)
            }

            override fun dispose() {
                audioPlayers.values.forEach { it.unload() }
                audioPlayers.clear()
            }

            private fun AVAudioPlayer.unload() {
                stop()
            }
        }
    }
}