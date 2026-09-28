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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.math.roundToInt

/**
 * Streams one MP3 track. Every playback job builds its own decoder chain (decoder, audio device and bitstream) and
 * closes it when it ends, so a cancelled job can never touch the chain of the job that replaced it.
 */
internal class DesktopMusicPlayer(
    inputStream: InputStream,
) {
    // Buffer the entire input stream so that we can rewind / restart playback cheaply.
    private val audioData: ByteArray = inputStream.use(InputStream::readBytes)

    private val lock = Any()

    @Volatile
    private var musicPlayingJob: Job? = null

    // A flow rather than a flag, so the decoding loop can suspend on a resume instead of polling for one.
    private val isMusicPaused = MutableStateFlow(false)

    @Volatile
    private var shouldLoop = false

    @Volatile
    private var leftVolume = 1f

    @Volatile
    private var rightVolume = 1f

    val isPlaying get() = musicPlayingJob?.isActive == true && !isMusicPaused.value

    /** Starts playback; when [shouldRestart] is true we rewind to the beginning before playing. */
    fun play(scope: CoroutineScope, shouldLoop: Boolean, shouldRestart: Boolean) = synchronized(lock) {
        this.shouldLoop = shouldLoop
        if (shouldRestart) {
            cancelCurrentJob()
        }
        isMusicPaused.value = false
        if (musicPlayingJob == null) {
            musicPlayingJob = startPlayback(scope)
        }
    }

    private fun startPlayback(scope: CoroutineScope) = scope.launch(Dispatchers.Default) {
        var bitstream: Bitstream? = null
        var audioDevice: AudioDevice? = null
        var hasEndedOnItsOwn = false
        try {
            val decoder = Decoder()
            val device = FactoryRegistry.systemRegistry().createAudioDevice().also { it.open(decoder) }
            audioDevice = device
            var currentBitstream = createBitstream()
            bitstream = currentBitstream
            do {
                var hasNextFrame: Boolean
                do {
                    ensureActive()
                    if (isMusicPaused.value) {
                        // Suspend until resumed rather than waking to re-read the flag; the decoder
                        // and the bitstream keep their position either way.
                        isMusicPaused.first { !it }
                        hasNextFrame = true
                        continue
                    }
                    hasNextFrame = playFrame(currentBitstream, decoder, device)
                } while (hasNextFrame && isActive)
                if (shouldLoop && isActive) {
                    // For loops we only rewind the bitstream so playback restarts from the first frame.
                    closeBitstream(currentBitstream)
                    currentBitstream = createBitstream()
                    bitstream = currentBitstream
                }
            } while (shouldLoop && isActive)
            hasEndedOnItsOwn = isActive
        } catch (_: JavaLayerException) {
        } finally {
            closeBitstream(bitstream)
            audioDevice?.let { device ->
                // A cancelled job cuts its audio instead of playing out the buffered tail over whatever comes next.
                if (hasEndedOnItsOwn) {
                    runCatching { device.flush() }
                }
                runCatching { device.close() }
            }
            synchronized(lock) {
                if (musicPlayingJob === coroutineContext.job) {
                    musicPlayingJob = null
                    isMusicPaused.value = false
                }
            }
        }
    }

    fun pause() {
        // Checked inside the playback loop – decoding stops while keeping the bitstream position.
        isMusicPaused.value = true
    }

    fun stop() = synchronized(lock) {
        cancelCurrentJob()
    }

    private fun cancelCurrentJob() {
        musicPlayingJob?.cancel()
        musicPlayingJob = null
        isMusicPaused.value = false
    }

    fun dispose() = stop()

    fun setVolume(leftVolume: Float, rightVolume: Float) {
        // Store the latest volume so that it can be applied on the next decoded frame.
        this.leftVolume = leftVolume
        this.rightVolume = rightVolume
    }

    private fun playFrame(bitstream: Bitstream, decoder: Decoder, audioDevice: AudioDevice): Boolean = try {
        val header: Header? = bitstream.readFrame()
        if (header == null) {
            false
        } else {
            val output = decoder.decodeFrame(header, bitstream) as SampleBuffer
            applyVolume(output)
            audioDevice.write(output.buffer, 0, output.bufferLength)
            bitstream.closeFrame()
            true
        }
    } catch (_: BitstreamException) {
        false
    } catch (_: JavaLayerException) {
        false
    } catch (_: ArrayIndexOutOfBoundsException) {
        false
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

    private fun createBitstream() = Bitstream(BufferedInputStream(ByteArrayInputStream(audioData)))

    private fun closeBitstream(bitstream: Bitstream?) {
        runCatching { bitstream?.close() } // Bitstream#close throws when already closed – swallow it.
    }
}
