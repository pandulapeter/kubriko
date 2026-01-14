/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
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
    private var channelSplitterNode: ChannelSplitterNode? = null
    private var channelMergerNode: ChannelMergerNode? = null
    private var leftGainNode: GainNode? = null
    private var rightGainNode: GainNode? = null
    var isPlaying = false
        private set
    private var pausedAt = 0.0
    private var startedAt = 0.0
    private var playJob: Job? = null
    private var leftVolume = 1f
    private var rightVolume = 1f

    init {
        scope.launch(Dispatchers.Default) {
            val response = window.fetch(uri).await<Response>()
            val arrayBuffer = response.arrayBuffer().await<ArrayBuffer>()
            audioBuffer = AudioContext().decodeAudioData(arrayBuffer).await()
            onPreloadReady(this@WebMusicPlayer)
        }
    }

    /**
     * Starts playback – when [shouldRestart] is true we rewind the position before starting.
     */
    fun play(shouldLoop: Boolean, shouldRestart: Boolean) {
        if (shouldRestart && isPlaying) {
            stopInternal(resetPosition = true)
        }

        if (playJob == null && !isPlaying) {
            playJob = scope.launch(Dispatchers.Default) {
                rebuildAudioGraph(shouldLoop)
                sourceNode?.start(0.0, pausedAt)
                startedAt = (audioContext?.currentTime ?: 0.0) - pausedAt
                isPlaying = true
            }
        } else {
            sourceNode?.loop = shouldLoop
        }
    }

    fun pause() {
        if (!isPlaying) return
        pausedAt = (audioContext?.currentTime ?: 0.0) - startedAt
        stopInternal(resetPosition = false)
    }

    fun stop() {
        stopInternal(resetPosition = true)
    }

    fun setVolume(leftVolume: Float, rightVolume: Float) {
        this.leftVolume = leftVolume.coerceIn(0f, 1f)
        this.rightVolume = rightVolume.coerceIn(0f, 1f)
        applyVolumeToNodes()
    }

    fun dispose() {
        stopInternal(resetPosition = true)
        audioBuffer = null
        audioContext = null
    }

    private fun stopInternal(resetPosition: Boolean) {
        playJob?.cancel()
        playJob = null
        sourceNode?.let {
            runCatching { it.stop() }
            it.disconnect()
            it.buffer = null
        }
        sourceNode = null
        tearDownAudioGraph()
        audioContext?.let {
            it.suspend()
            it.close()
        }
        audioContext = null
        isPlaying = false
        if (resetPosition) {
            pausedAt = 0.0
        }
        startedAt = 0.0
    }

    private fun rebuildAudioGraph(shouldLoop: Boolean) {
        tearDownAudioGraph()
        val buffer = audioBuffer ?: return
        val context = AudioContext().also { audioContext = it }
        val source = context.createBufferSource().apply {
            this.buffer = buffer
            loop = shouldLoop
        }
        val splitter = context.createChannelSplitter(2)
        val merger = context.createChannelMerger(2)
        val leftGain = context.createGain()
        val rightGain = context.createGain()

        source.connect(splitter)
        splitter.connect(leftGain, output = 0)
        splitter.connect(rightGain, output = 1)
        leftGain.connect(merger, output = 0, input = 0)
        rightGain.connect(merger, output = 0, input = 1)
        merger.connect(context.destination)

        sourceNode = source
        channelSplitterNode = splitter
        channelMergerNode = merger
        leftGainNode = leftGain
        rightGainNode = rightGain
        applyVolumeToNodes()
    }

    private fun applyVolumeToNodes() {
        leftGainNode?.gain?.value = leftVolume.toDouble()
        rightGainNode?.gain?.value = rightVolume.toDouble()
    }

    private fun tearDownAudioGraph() {
        channelSplitterNode?.disconnect()
        channelMergerNode?.disconnect()
        leftGainNode?.disconnect()
        rightGainNode?.disconnect()
        channelSplitterNode = null
        channelMergerNode = null
        leftGainNode = null
        rightGainNode = null
    }
}

internal external class AudioContext {
    fun decodeAudioData(audioData: ArrayBuffer): Promise<AudioBuffer>
    fun createBufferSource(): AudioBufferSourceNode
    fun createGain(): GainNode
    fun createChannelSplitter(numberOfOutputs: Int = definedExternally): ChannelSplitterNode
    fun createChannelMerger(numberOfInputs: Int = definedExternally): ChannelMergerNode
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

internal external class GainNode : AudioNode {
    val gain: AudioParam
}

internal external class ChannelSplitterNode : AudioNode

internal external class ChannelMergerNode : AudioNode

internal external class AudioParam {
    var value: Double
}

internal abstract external class AudioNode {
    fun connect(destinationNode: AudioNode, output: Int = definedExternally, input: Int = definedExternally)
    fun disconnect()
}

external class ArrayBuffer
