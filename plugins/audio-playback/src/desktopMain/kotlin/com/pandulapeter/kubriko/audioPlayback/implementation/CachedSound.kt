/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/。
 */
package com.pandulapeter.kubriko.audioPlayback.implementation

import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentLinkedQueue
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent

/**
 * Represents a cached sound with a pool of pre-loaded clips for simultaneous playback.
 */
internal class CachedSound(
    val uri: String,
    private val audioData: ByteArray,
    private val audioFormat: AudioFormat,
    private val maxSimultaneousStreams: Int
) {
    private val clipPool = ConcurrentLinkedQueue<Clip>()
    private val activeClips = mutableSetOf<Clip>()

    init {
        // Pre-create clips up to the limit
        repeat(maxSimultaneousStreams) {
            createAndAddClip()
        }
    }

    private fun createAndAddClip(): Clip? {
        return try {
            val clip = AudioSystem.getClip()
            val stream = AudioInputStream(
                ByteArrayInputStream(audioData),
                audioFormat,
                audioData.size.toLong() / audioFormat.frameSize
            )
            clip.open(stream)
            clip.addLineListener { event ->
                if (event.type == LineEvent.Type.STOP) {
                    returnClipToPool(clip)
                }
            }
            clipPool.offer(clip)
            clip
        } catch (e: Exception) {
            null
        }
    }

    fun getAvailableClip(): Clip? {
        val clip = clipPool.poll()
        if (clip != null) {
            synchronized(activeClips) {
                activeClips.add(clip)
            }
            clip.framePosition = 0
        }
        return clip
    }

    private fun returnClipToPool(clip: Clip) {
        synchronized(activeClips) {
            if (activeClips.remove(clip)) {
                clip.framePosition = 0
                clipPool.offer(clip)
            }
        }
    }

    fun dispose() {
        clipPool.forEach { clip ->
            runCatching { clip.stop() }
            runCatching { clip.close() }
        }
        clipPool.clear()

        synchronized(activeClips) {
            activeClips.forEach { clip ->
                runCatching { clip.stop() }
                runCatching { clip.close() }
            }
            activeClips.clear()
        }
    }
}
