package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLAudioElement
import org.w3c.fetch.Response

@Composable
internal actual fun createAudioPlayer(coroutineScope: CoroutineScope) = object : AudioPlayer {
    private val audioContext = AudioContext()
    private var audioBuffer: AudioBuffer? = null
    private var sourceNode: AudioBufferSourceNode? = null
    private var isPlaying = false
    private var pausedAt = 0.0
    private var startedAt = 0.0
    private var shouldLoop = false
    private var playJob: Job? = null

    override fun playMusic(uri: String, shouldLoop: Boolean) {
        stopMusic()
        this.shouldLoop = shouldLoop
        playJob = coroutineScope.launch(Dispatchers.Default) {
            val response = window.fetch(uri).await<Response>()
            val arrayBuffer = response.arrayBuffer().await<ArrayBuffer>()
            audioBuffer = audioContext.decodeAudioData(arrayBuffer).await()
            resumeMusic()
        }
    }

    override fun resumeMusic() {
        if (!isPlaying) {
            sourceNode = audioContext.createBufferSource().apply {
                buffer = audioBuffer
                loop = shouldLoop
                connect(audioContext.destination)
            }
            startedAt = audioContext.currentTime - pausedAt
            sourceNode?.start(0.0, pausedAt)
            isPlaying = true
        }
        playJob = null
    }

    override fun pauseMusic() {
        playJob?.cancel()
        if (isPlaying) {
            pausedAt = audioContext.currentTime - startedAt
        }
        sourceNode?.stop()
        isPlaying = false
    }

    override fun stopMusic() {
        audioBuffer = null
        sourceNode?.stop()
        sourceNode?.disconnect()
        sourceNode = null
        sourceNode = null
        isPlaying = false
        pausedAt = 0.0
        startedAt = 0.0
    }

    override fun playSound(uri: String) {
        coroutineScope.launch(Dispatchers.Default) {
            (document.createElement("audio") as HTMLAudioElement).apply {
                src = uri
            }.play()
        }
    }

    override fun dispose() {
        stopMusic()
    }

    private fun HTMLAudioElement.unload() {
        if (!paused) {
            pause()
        }
        src = ""
        remove()
    }
}