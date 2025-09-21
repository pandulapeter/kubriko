/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
@file:OptIn(ExperimentalWasmJsInterop::class)

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
    private var audioContext: AudioContext? = null
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
            audioBuffer = AudioContext().decodeAudioData(arrayBuffer).await()
            onPreloadReady(this@WebMusicPlayer)
        }
    }

    fun play(shouldLoop: Boolean) {
        if (playJob == null && !isPlaying) {
            playJob = scope.launch(Dispatchers.Default) {
                sourceNode?.let {
                    it.stop()
                    it.disconnect()
                    it.buffer = null
                }
                audioContext?.suspend()
                audioContext?.close()
                audioContext = AudioContext().apply {
                    sourceNode = createBufferSource().apply {
                        buffer = audioBuffer
                        loop = shouldLoop
                        connect(destination)
                        start(0.0, pausedAt)
                    }
                    startedAt = currentTime - pausedAt
                }
                isPlaying = true
            }
        } else {
            sourceNode?.loop = shouldLoop
        }
    }

    fun pause() {
        playJob?.cancel()
        playJob = null
        if (isPlaying) {
            pausedAt = (audioContext?.currentTime ?: 0.0) - startedAt
        }
        sourceNode?.stop()
        isPlaying = false
    }

    fun stop() {
        pause()
        sourceNode?.let {
            it.stop()
            it.disconnect()
            it.buffer = null
        }
        sourceNode = null
        audioContext?.suspend()
        audioContext?.close()
        audioContext = null
        pausedAt = 0.0
        startedAt = 0.0
    }

    fun dispose() {
        stop()
        audioBuffer = null
        audioContext = null
    }
}

internal external class AudioContext {
    fun decodeAudioData(audioData: ArrayBuffer): Promise<AudioBuffer>
    fun createBufferSource(): AudioBufferSourceNode
    fun suspend()
    fun close()
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