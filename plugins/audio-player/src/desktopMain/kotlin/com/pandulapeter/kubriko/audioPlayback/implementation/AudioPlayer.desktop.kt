package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.net.URI
import javax.sound.sampled.AudioSystem


@Composable
internal actual fun rememberAudioPlayer(): AudioPlayer {
    val scope = rememberCoroutineScope()
    return remember {
        object : AudioPlayer {

            override fun playSound(uri: String) {
                val clip = AudioSystem.getClip()
                scope.launch(Dispatchers.IO) {
                    val inputStream = URI(uri).let { uri ->
                        if (uri.isAbsolute) {
                            uri.toURL().openStream()
                        } else {
                            FileInputStream(uri.toString())
                        }
                    }
                    clip.open(AudioSystem.getAudioInputStream(BufferedInputStream(inputStream)))
                    clip.start()
                    do {
                        delay(100)
                    } while (clip.isRunning)
                    clip.close()
                }
            }

            override fun dispose() = Unit
        }

    }
}