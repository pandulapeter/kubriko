/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
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
import javazoom.jl.decoder.JavaLayerException
import javazoom.jl.decoder.SampleBuffer
import javazoom.jl.player.AudioDevice
import javazoom.jl.player.FactoryRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream

internal class DesktopMusicPlayer(
    inputStream: InputStream?,
) {
    private val audioDevice = FactoryRegistry.systemRegistry().createAudioDevice()
    private val bitstream = Bitstream(inputStream)
    private var closed = false
    private val decoder = Decoder()
    private var isMusicPaused = false
    private var shouldLoop = false
    private var musicPlayingJob: Job? = null
    val isPlaying get() = musicPlayingJob?.isActive == true && !isMusicPaused

    init {
        audioDevice.open(decoder)
    }

    fun play(scope: CoroutineScope, shouldLoop: Boolean) {
        if (!isPlaying) {
            isMusicPaused = false
            this.shouldLoop = shouldLoop
            if (musicPlayingJob == null) {
                musicPlayingJob = scope.launch(Dispatchers.Default) {
                    do {
                        var isThereANextFrame = true
                        do {
                            if (isMusicPaused) {
                                delay(10)
                            } else {
                                isThereANextFrame = playFrame()
                            }
                        } while (isThereANextFrame && isActive && !closed)
                    } while (this@DesktopMusicPlayer.shouldLoop && isActive && !closed)
                    stop()
                }
            }
        }
    }

    fun pause() {
        isMusicPaused = true
    }

    @Throws(JavaLayerException::class)
    private fun playFrame(): Boolean {
        val hasNextFrame = decodeFrame()
        if (!hasNextFrame) {
            synchronized(this) {
                close()
            }
        }
        return hasNextFrame
    }

    // TODO: Doesn't work as stop, it just pauses the stream
    fun stop() {
        pause()
        musicPlayingJob?.cancel()
        musicPlayingJob = null
    }

    fun dispose() {
        stop()
        audioDevice.flush()
    }

    @Synchronized
    fun close() {
        stop()
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