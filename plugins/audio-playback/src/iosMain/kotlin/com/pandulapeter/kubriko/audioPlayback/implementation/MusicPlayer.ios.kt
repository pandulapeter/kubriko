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

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSURL
import platform.darwin.NSIntegerMax

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun createMusicPlayer(coroutineScope: CoroutineScope) = object : MusicPlayer {

    init {
        AVAudioSession.sharedInstance().apply {
            setCategory(AVAudioSessionCategoryPlayback, error = null)
            setActive(true, error = null)
        }
    }

    override suspend fun preload(uri: String) = withContext(Dispatchers.Default) {
        AVAudioPlayer(NSURL.URLWithString(URLString = uri)!!, error = null).apply {
            prepareToPlay()
        }
    }

    override suspend fun play(cachedMusic: Any, shouldLoop: Boolean, shouldRestart: Boolean) {
        cachedMusic as AVAudioPlayer
        cachedMusic.setNumberOfLoops(if (shouldLoop) NSIntegerMax else 0)
        
        // Handle restart request
        if (shouldRestart && cachedMusic.isPlaying()) {
            cachedMusic.stop()
            cachedMusic.setCurrentTime(0.0)
        }
        
        if (!cachedMusic.isPlaying()) {
            cachedMusic.play()
        }
    }

    override fun isPlaying(cachedMusic: Any) = (cachedMusic as AVAudioPlayer).isPlaying()

    override fun pause(cachedMusic: Any) {
        cachedMusic as AVAudioPlayer
        if (cachedMusic.isPlaying()) {
            cachedMusic.pause()
        }
    }

    // TODO: Works like a pause, not a stop
    override fun stop(cachedMusic: Any) {
        cachedMusic as AVAudioPlayer
        if (cachedMusic.isPlaying()) {
            cachedMusic.stop()
        }
        cachedMusic.setCurrentTime(0.0) // Reset to beginning
    }

    override fun setVolume(cachedMusic: Any, leftVolume: Float, rightVolume: Float) {
        cachedMusic as AVAudioPlayer
        // Overall volume as average of left and right
        val volume = (leftVolume + rightVolume) / 2f
        cachedMusic.setVolume(volume)
        
        // Pan: -1.0 (left) to 1.0 (right)
        // Calculate pan from the balance between left and right volumes
        val pan = if (leftVolume + rightVolume > 0f) {
            (rightVolume - leftVolume) / (leftVolume + rightVolume)
        } else {
            0f
        }
        cachedMusic.setPan(pan)
    }

    override fun dispose(cachedMusic: Any) = stop(cachedMusic)

    override fun dispose() = Unit
}

internal actual val musicPauseDelayOnFocusLoss: Long = 0L
