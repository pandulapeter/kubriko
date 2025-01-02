package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import javazoom.jl.player.FactoryRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.net.URI
import javax.sound.sampled.AudioSystem

@Composable
internal actual fun createAudioPlayer(coroutineScope: CoroutineScope) = object : AudioPlayer {
    private var musicPlayer: MusicPlayer? = null
    private var musicPlayingJob: Job? = null
    private var isMusicPaused = false
    private val audioDevice by lazy { FactoryRegistry.systemRegistry().createAudioDevice() }

    override fun playMusic(uri: String, shouldLoop: Boolean) {
        stopMusic()
        isMusicPaused = false
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
                musicPlayer = MusicPlayer(inputStream, audioDevice)
                musicPlayer?.run {
                    var isThereANextFrame = true
                    do {
                        if (isMusicPaused) {
                            delay(10)
                        } else {
                            isThereANextFrame = play(1)
                        }
                    } while (isThereANextFrame && isActive)
                }
            } while (shouldLoop && isActive && musicPlayer != null)
        }
    }

    override fun resumeMusic() {
        isMusicPaused = false
    }

    override fun pauseMusic() {
        isMusicPaused = true
    }

    override fun stopMusic() {
        isMusicPaused = true
        musicPlayer?.close()
        musicPlayer = null
        musicPlayingJob?.cancel()
        musicPlayingJob = null
    }

    override fun playSound(uri: String) {
        coroutineScope.launch(Dispatchers.IO) {
            AudioSystem.getClip().apply {
                val inputStream = URI(uri).let { uri ->
                    if (uri.isAbsolute) {
                        uri.toURL().openStream()
                    } else {
                        FileInputStream(uri.toString())
                    }
                }
                open(AudioSystem.getAudioInputStream(BufferedInputStream(inputStream)))
                start()
            }
        }
    }

    override fun dispose() {
        coroutineScope.launch(Dispatchers.Default) {
            stopMusic()
            audioDevice.flush()
        }
    }
}