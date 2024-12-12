package com.pandulapeter.kubriko.audioPlayback.implementation

import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.BitstreamException
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.JavaLayerException
import javazoom.jl.decoder.SampleBuffer
import javazoom.jl.player.AudioDevice
import java.io.InputStream

internal class MusicPlayer(
    stream: InputStream?,
    private val audioDevice: AudioDevice,
) {
    private val bitstream = Bitstream(stream)
    private var closed = false
    private val decoder = Decoder()

    @get:Synchronized
    var isComplete: Boolean = false
        private set

    init {
        audioDevice.open(decoder)
    }

    @Throws(JavaLayerException::class)
    fun play(frames: Int): Boolean {
        var currentFrame = frames
        var ret = true
        while (currentFrame-- > 0 && ret) {
            ret = decodeFrame()
        }
        if (!ret) {
            synchronized(this) {
                isComplete = (!closed)
                close()
            }
        }
        return ret
    }

    @Synchronized
    fun close() {
        closed = true
        try {
            bitstream.close()
        } catch (_: BitstreamException) {
        }
    }

    @Throws(JavaLayerException::class)
    private fun decodeFrame(): Boolean {
        try {
            val h = bitstream.readFrame() ?: return false
            val output = decoder.decodeFrame(h, bitstream) as SampleBuffer
            val out: AudioDevice
            synchronized(this) {
                out = audioDevice
                out.write(output.buffer, 0, output.bufferLength)
            }
            bitstream.closeFrame()
        } catch (ex: RuntimeException) {
            throw JavaLayerException("Exception decoding audio frame", ex)
        }
        return true
    }
}