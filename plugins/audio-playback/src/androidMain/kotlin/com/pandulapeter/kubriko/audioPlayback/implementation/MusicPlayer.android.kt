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
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
internal actual fun createMusicPlayer(coroutineScope: CoroutineScope) = object : MusicPlayer {
    private val context = LocalContext.current.applicationContext

    private fun Context.getFileDescriptor(uri: String) = assets.openFd(uri.removePrefix("file:///android_asset/"))

    override suspend fun preload(uri: String) = withContext(Dispatchers.IO) {
        suspendCoroutine { continuation ->
            MediaPlayer().apply {
                val fileDescriptor = context.getFileDescriptor(uri)
                setDataSource(
                    fileDescriptor.fileDescriptor,
                    fileDescriptor.startOffset,
                    fileDescriptor.length
                )
                fileDescriptor.close()
                setOnPreparedListener {
                    continuation.resume(this)
                }
                prepare()
            }
        }
    }

    override suspend fun play(cachedMusic: Any, shouldLoop: Boolean, shouldRestart: Boolean) = withContext(Dispatchers.Default) {
        (cachedMusic as MediaPlayer).run {
            // If shouldRestart is true and already playing, reset to beginning
            if (shouldRestart && isPlaying) {
                pause()
                seekTo(0)
            }
            
            if (shouldLoop) {
                isLooping = true
                setOnCompletionListener(null)
            } else {
                setOnCompletionListener { 
                    pause()
                    seekTo(0)
                }
            }
            if (!isPlaying) {
                start()
            }
        }
    }

    override fun isPlaying(cachedMusic: Any) = (cachedMusic as MediaPlayer).isPlaying

    override fun pause(cachedMusic: Any) = (cachedMusic as MediaPlayer).pause()

    override fun stop(cachedMusic: Any) = (cachedMusic as MediaPlayer).run {
        pause()
        seekTo(0)
    }

    override fun setVolume(cachedMusic: Any, leftVolume: Float, rightVolume: Float) {
        (cachedMusic as MediaPlayer).setVolume(leftVolume, rightVolume)
    }

    override fun dispose(cachedMusic: Any) {
        stop(cachedMusic)
        (cachedMusic as MediaPlayer).release()
    }

    override fun dispose() = Unit
}

internal actual val musicPauseDelayOnFocusLoss: Long = 0L
