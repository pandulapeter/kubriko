package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import javazoom.jl.player.advanced.AdvancedPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.net.URI
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip


@Composable
internal actual fun rememberAudioPlayer(): AudioPlayer {
    val coroutineScope = rememberCoroutineScope()
    return remember {
        object : AudioPlayer {
            private val clips = mutableMapOf<String, Clip>()
            private var musicPlayer: AdvancedPlayer? = null
            private var musicPlayingJob: Job? = null

            override fun playMusic(uri: String, shouldLoop: Boolean) {
                stopMusic()
                musicPlayingJob = coroutineScope.launch(Dispatchers.Default) {
                    do {
                        val inputStream = URI(uri).let { uri ->
                            if (uri.isAbsolute) {
                                uri.toURL().openStream()
                            } else {
                                FileInputStream(uri.toString())
                            }
                        }
                        musicPlayer?.close()
                        musicPlayer = AdvancedPlayer(inputStream)
                        musicPlayer?.play()
                    } while (shouldLoop && isActive)
                }
            }

            override fun resumeMusic() {
                // TODO
            }

            override fun pauseMusic() {
                // TODO
            }

            override fun stopMusic() {
                musicPlayer?.stop()
                musicPlayingJob?.cancel()
                musicPlayingJob = null
                musicPlayer = null
            }

            private fun preloadSound(uri: String) {
                coroutineScope.launch(Dispatchers.IO) {
                    if (clips[uri] == null) {
                        clips[uri] = AudioSystem.getClip().apply {
                            val inputStream = URI(uri).let { uri ->
                                if (uri.isAbsolute) {
                                    uri.toURL().openStream()
                                } else {
                                    FileInputStream(uri.toString())
                                }
                            }
                            open(AudioSystem.getAudioInputStream(BufferedInputStream(inputStream)))
                        }
                    }
                }
            }

            override fun preloadSounds(uris: Collection<String>) = uris.forEach(::preloadSound)

            override fun playSound(uri: String) {
                clips[uri].let { clip ->
                    if (clip == null) {
                        coroutineScope.launch {
                            preloadSound(uri)
                            do {
                                delay(50)
                            } while (clips[uri] == null)
                        }
                        playSound(uri)
                    } else {
                        clip.framePosition = 0
                        clip.start()
                    }
                }
            }

            override fun unloadSounds(uris: Collection<String>) = uris.forEach { uri ->
                clips[uri]?.unload()
                clips.remove(uri)
            }

            override fun dispose() {
                stopMusic()
                clips.values.forEach { it.unload() }
                clips.clear()
            }

            private fun Clip.unload() {
                stop()
                close()
            }
        }
    }
}