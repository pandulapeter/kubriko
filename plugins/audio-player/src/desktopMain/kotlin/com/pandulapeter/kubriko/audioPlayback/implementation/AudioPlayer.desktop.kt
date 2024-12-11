package com.pandulapeter.kubriko.audioPlayback.implementation

import javazoom.jl.player.Player
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
import javax.sound.sampled.Clip


internal actual fun createAudioPlayer(coroutineScope: CoroutineScope) = object : AudioPlayer {
    private val clips = mutableMapOf<String, Clip>()
    private var musicPlayer: Player? = null
    private var musicPlayingJob: Job? = null
    private var isMusicPaused = false

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
                musicPlayer = Player(inputStream)
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
                    } while (isActive && clips[uri] == null)
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