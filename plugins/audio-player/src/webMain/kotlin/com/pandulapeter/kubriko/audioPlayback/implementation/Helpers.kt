package com.pandulapeter.kubriko.audioPlayback.implementation

import kotlin.js.Promise

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