package com.pandulapeter.kubriko.audioPlayback.implementation

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
internal actual fun createAudioPlayer(coroutineScope: CoroutineScope) = object : AudioPlayer {
    private val audioPlayers = mutableMapOf<String, AVAudioPlayer>()
    private var musicPlayer: AVAudioPlayer? = null

    private fun preloadSound(uri: String) {
        coroutineScope.launch(Dispatchers.Default) {
            if (audioPlayers[uri] == null) {
                audioPlayers[uri] = AVAudioPlayer(NSURL.URLWithString(URLString = uri)!!, error = null).apply {
                    prepareToPlay()
                }
            }
        }
    }

    override fun playMusic(uri: String, shouldLoop: Boolean) {
        stopMusic()
        coroutineScope.launch(Dispatchers.Default) {
            musicPlayer = AVAudioPlayer(NSURL.URLWithString(URLString = uri)!!, error = null).apply {
                prepareToPlay()
                play()
            }
        }
    }

    override fun resumeMusic() {
        musicPlayer?.run {
            if (!isPlaying()) {
                play()
            }
        }
    }

    override fun pauseMusic() {
        musicPlayer?.run {
            if (isPlaying()) {
                pause()
            }
        }
    }

    override fun stopMusic() {
        musicPlayer?.stop()
    }

    override fun preloadSounds(uris: Collection<String>) = uris.forEach(::preloadSound)

    override fun playSound(uri: String) {
        audioPlayers[uri].let { audioPlayer ->
            if (audioPlayer == null) {
                coroutineScope.launch(Dispatchers.Default) {
                    preloadSound(uri)
                    do {
                        delay(50)
                    } while (audioPlayers[uri] == null)
                    playSound(uri)
                }
            } else {
                coroutineScope.launch(Dispatchers.Default) {
                    audioPlayer.play()
                }
            }
        }
    }

    override fun unloadSounds(uris: Collection<String>) = uris.forEach { uri ->
        audioPlayers[uri]?.unload()
        audioPlayers.remove(uri)
    }

    override fun dispose() {
        stopMusic()
        audioPlayers.values.forEach { it.unload() }
        audioPlayers.clear()
    }

    private fun AVAudioPlayer.unload() {
        stop()
    }
}