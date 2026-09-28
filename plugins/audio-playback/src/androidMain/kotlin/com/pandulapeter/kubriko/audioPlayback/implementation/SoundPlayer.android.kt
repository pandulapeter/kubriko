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
    private val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(maximumSimultaneousStreamsOfTheSameSound)
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
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val sampleId = fileDescriptor.use { soundPool.load(it, 1) }
                if (sampleId == 0) {
                    continuation.resume(null)
                } else {
                    pendingLoads[sampleId] = continuation
                }
            }
        }
    }

    override suspend fun play(sound: Any) {
        withContext(Dispatchers.Default) {
            soundPool.play(sound as Int, 1f, 1f, 1, 0, 1f)
        }
    }

    override fun dispose(cachedSound: Any) {
        cachedSound as Int
        soundPool.stop(cachedSound)
        soundPool.unload(cachedSound)
    }

    override fun dispose() = soundPool.release()
}
