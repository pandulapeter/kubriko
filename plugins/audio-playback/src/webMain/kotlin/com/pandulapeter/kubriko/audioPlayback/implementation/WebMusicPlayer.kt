package com.pandulapeter.kubriko.audioPlayback.implementation

import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.w3c.fetch.Response
import kotlin.js.Promise

internal class WebMusicPlayer(
    private val scope: CoroutineScope,
    private val uri: String,
    onPreloadReady: (WebMusicPlayer) -> Unit,
) {
    private val audioContext = AudioContext()
    private var audioBuffer: AudioBuffer? = null
    private var sourceNode: AudioBufferSourceNode? = null
    var isPlaying = false
        private set
    private var pausedAt = 0.0
    private var startedAt = 0.0
    private var playJob: Job? = null

    init {
        scope.launch(Dispatchers.Default) {
            val response = window.fetch(uri).await<Response>()
            val arrayBuffer = response.arrayBuffer().await<ArrayBuffer>()
            audioBuffer = audioContext.decodeAudioData(arrayBuffer).await()
            onPreloadReady(this@WebMusicPlayer)
        }
    }

    fun play(shouldLoop: Boolean) {
        playJob = scope.launch(Dispatchers.Default) {
            if (!isPlaying) {
                sourceNode = audioContext.createBufferSource().apply {
                    buffer = audioBuffer
                    connect(audioContext.destination)
                }
                startedAt = audioContext.currentTime - pausedAt
                sourceNode?.start(0.0, pausedAt)
                isPlaying = true
            }
        }
        sourceNode?.loop = shouldLoop
    }

    fun pause() {
        playJob?.cancel()
        if (isPlaying) {
            pausedAt = audioContext.currentTime - startedAt
        }
        sourceNode?.stop()
        isPlaying = false
    }

    fun stop() {
        pause()
        audioBuffer = null
        sourceNode?.disconnect()
        sourceNode = null
        pausedAt = 0.0
        startedAt = 0.0
    }
}

internal external class AudioContext {
    fun decodeAudioData(audioData: ArrayBuffer): Promise<AudioBuffer>
    fun createBufferSource(): AudioBufferSourceNode
    val destination: AudioNode
    val currentTime: Double
}

internal external class AudioBuffer : JsAny

internal external class AudioBufferSourceNode : AudioNode {
    var buffer: AudioBuffer?
    var loop: Boolean
    fun start(time: Double = definedExternally, resume: Double = definedExternally)
    fun stop(time: Double = definedExternally)
}

internal abstract external class AudioNode {
    fun connect(destinationNode: AudioNode)
    fun disconnect()
}

external class ArrayBuffer