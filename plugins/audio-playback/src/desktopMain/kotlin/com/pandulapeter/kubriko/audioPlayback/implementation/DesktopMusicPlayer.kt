/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.audioPlayback.implementation

import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.BitstreamException
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.Header
import javazoom.jl.decoder.JavaLayerException
import javazoom.jl.decoder.SampleBuffer
import javazoom.jl.player.AudioDevice
import javazoom.jl.player.FactoryRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.math.roundToInt

internal class DesktopMusicPlayer(
    inputStream: InputStream?,
) {
    // Buffer the entire input stream so that we can rewind / restart playback cheaply.
    private val audioData: ByteArray = inputStream?.use(InputStream::readBytes)
        ?: throw IllegalArgumentException("Desktop music player requires a non-null input stream.")

    private var audioDevice: AudioDevice? = null
    private var decoder: Decoder? = null
    private var bitstream: Bitstream? = null
    private var musicPlayingJob: Job? = null
    private var isMusicPaused = false
    private var shouldLoop = false

    @Volatile
    private var leftVolume = 1f

    @Volatile
    private var rightVolume = 1f

    val isPlaying get() = musicPlayingJob?.isActive == true && !isMusicPaused

    init {
        rebuildDecoderChain()
    }

    /** Starts playback; when [shouldRestart] is true we rewind to the beginning before playing. */
    fun play(scope: CoroutineScope, shouldLoop: Boolean, shouldRestart: Boolean) {
        this.shouldLoop = shouldLoop

        if (shouldRestart) {
            // Cancel current playback + fully rebuild the decoder chain before restarting.
            stop()
        }

        if (musicPlayingJob == null) {
            startPlayback(scope)
        } else {
            // Resume from pause without recreating the coroutine.
            isMusicPaused = false
        }
    }

    private fun startPlayback(scope: CoroutineScope) {
        musicPlayingJob = scope.launch(Dispatchers.Default) {
            try {
                do {
                    var hasNextFrame: Boolean=false
                    do {
                        ensureActive()
                        if (isMusicPaused) {
                            delay(16)
                            continue
                        }
                        hasNextFrame = playFrame()
                    } while (hasNextFrame && isActive)
                    if (shouldLoop && isActive) {
                        // For loops we only rewind the bitstream so playback restarts from the first frame.
                        rewindBitstreamOnly()
                    }
                } while (shouldLoop && isActive)
            } finally {
                // Make sure we are ready for the next invocation once the coroutine finishes.
                rebuildDecoderChain()
                isMusicPaused = false
                musicPlayingJob = null
            }
        }
    }

    fun pause() {
        // Flag checked inside the playback loop – decoding stops while keeping the bitstream position.
        isMusicPaused = true
    }

    fun stop() {
        // Cancel the decoding coroutine and rebuild the decoder/device so the next playback starts clean.
        musicPlayingJob?.cancel()
        musicPlayingJob = null
        isMusicPaused = false
        rebuildDecoderChain()
    }

    fun dispose() {
        stop()
        closeAudioDevice()
        closeBitstream()
    }

    fun setVolume(leftVolume: Float, rightVolume: Float) {
        // Store the latest volume so that it can be applied on the next decoded frame.
        this.leftVolume = leftVolume
        this.rightVolume = rightVolume
    }

    private fun playFrame(): Boolean {
        val currentBitstream = bitstream ?: return false
        val currentDecoder = decoder ?: return false
        val currentAudioDevice = audioDevice ?: return false
        return try {
            val header: Header = currentBitstream.readFrame() ?: return false
            val output = currentDecoder.decodeFrame(header, currentBitstream) as SampleBuffer
            applyVolume(output)
            synchronized(currentAudioDevice) {
                currentAudioDevice.write(output.buffer, 0, output.bufferLength)
            }
            currentBitstream.closeFrame()
            true
        } catch (_: BitstreamException) {
            false
        } catch (_: JavaLayerException) {
            false
        }
    }

    /**
     * Applies per-channel gain so that left / right sliders behave the same way as on Android.
     */
    private fun applyVolume(buffer: SampleBuffer) {
        val channels = buffer.channelCount
        val array = buffer.buffer
        if (channels <= 0) return

        val clampedLeft = leftVolume.coerceIn(0f, 1f)
        val clampedRight = rightVolume.coerceIn(0f, 1f)
        if (channels == 1) {
            // Mono streams get the average gain to remain centered.
            val gain = (clampedLeft + clampedRight) / 2f
            for (index in 0 until buffer.bufferLength) {
                array[index] = scaleSample(array[index], gain)
            }
        } else {
            for (index in 0 until buffer.bufferLength) {
                val gain = if (index % channels == 0) clampedLeft else clampedRight
                array[index] = scaleSample(array[index], gain)
            }
        }
    }

    private fun scaleSample(sample: Short, gain: Float): Short {
        val scaled = (sample.toInt() * gain).roundToInt()
        return scaled.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    private fun rebuildDecoderChain() {
        closeBitstream()
        closeAudioDevice()
        val newDecoder = Decoder()
        val newDevice = FactoryRegistry.systemRegistry().createAudioDevice().also { device ->
            device.open(newDecoder)
        }
        decoder = newDecoder
        audioDevice = newDevice
        bitstream = createBitstream()
    }

    private fun rewindBitstreamOnly() {
        closeBitstream()
        bitstream = createBitstream()
    }

    private fun createBitstream() = Bitstream(BufferedInputStream(ByteArrayInputStream(audioData)))

    private fun closeAudioDevice() {
        audioDevice?.let { device ->
            runCatching { device.flush() }
            runCatching { device.close() }
        }
        audioDevice = null
        decoder = null
    }

    private fun closeBitstream() {
        runCatching { bitstream?.close() } // Bitstream#close throws when already closed – swallow it.
        bitstream = null
    }
}
