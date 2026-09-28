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

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume

@Composable
internal actual fun createSoundPlayer(
    maximumSimultaneousStreamsOfTheSameSound: Int,
) = object : SoundPlayer {
    private val context = LocalContext.current.applicationContext

    /** Loads waiting for [SoundPool]'s completion callback, keyed by sample id. Only touched on the main thread. */
    private val pendingLoads = HashMap<Int, CancellableContinuation<Int?>>()

    /** The most recent stream ids of each loaded sample, so a sample can't hold more than its share of streams. */
    private val streamRings = HashMap<Int, StreamRing>()
    private val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(MAXIMUM_TOTAL_STREAMS)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
            .apply {
                setOnLoadCompleteListener { pool, sampleId, status ->
                    val continuation = pendingLoads.remove(sampleId) ?: return@setOnLoadCompleteListener
                    if (status == 0 && continuation.isActive) {
                        continuation.resume(sampleId) { _, _, _ -> pool.unload(sampleId) }
                    } else {
                        pool.unload(sampleId)
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }
            }
    }

    private fun Context.getFileDescriptor(uri: String) = assets.openFd(uri.removePrefix("file:///android_asset/"))

    override suspend fun preload(uri: String): Int? {
        val fileDescriptor = withContext(Dispatchers.IO) {
            try {
                context.getFileDescriptor(uri)
            } catch (_: IOException) {
                null
            }
        } ?: return null
        // SoundPool delivers load callbacks on the main looper, so registering there keeps pendingLoads single-threaded.
        val loadedSampleId = withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val sampleId = fileDescriptor.use { soundPool.load(it, 1) }
                if (sampleId == 0) {
                    continuation.resume(null)
                } else {
                    pendingLoads[sampleId] = continuation
                }
            }
        }
        if (loadedSampleId != null) {
            synchronized(streamRings) {
                streamRings[loadedSampleId] = StreamRing(IntArray(maximumSimultaneousStreamsOfTheSameSound.coerceAtLeast(1)))
            }
        }
        return loadedSampleId
    }

    override suspend fun play(sound: Any) {
        withContext(Dispatchers.Default) {
            sound as Int
            synchronized(streamRings) {
                val ring = streamRings[sound]
                if (ring == null) {
                    soundPool.play(sound, 1f, 1f, 1, 0, 1f)
                } else {
                    // SoundPool reports no end of stream, so the oldest stream of this sample is cut off to make room.
                    val oldestStreamId = ring.streamIds[ring.cursor]
                    if (oldestStreamId != 0) {
                        soundPool.stop(oldestStreamId)
                    }
                    ring.streamIds[ring.cursor] = soundPool.play(sound, 1f, 1f, 1, 0, 1f)
                    ring.cursor = (ring.cursor + 1) % ring.streamIds.size
                }
            }
        }
    }

    override fun dispose(cachedSound: Any) {
        cachedSound as Int
        synchronized(streamRings) {
            streamRings.remove(cachedSound)
        }
        soundPool.stop(cachedSound)
        soundPool.unload(cachedSound)
    }

    override fun dispose() = soundPool.release()
}

private class StreamRing(
    val streamIds: IntArray,
) {
    var cursor = 0
}

private const val MAXIMUM_TOTAL_STREAMS = 32
