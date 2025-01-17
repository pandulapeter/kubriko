/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
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
import platform.Foundation.NSURL
import platform.darwin.NSIntegerMax

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun createMusicPlayer(coroutineScope: CoroutineScope) = object : MusicPlayer {

    override suspend fun preload(uri: String) = withContext(Dispatchers.Default) {
        AVAudioPlayer(NSURL.URLWithString(URLString = uri)!!, error = null).apply {
            prepareToPlay()
        }
    }

    override suspend fun play(cachedMusic: Any, shouldLoop: Boolean) {
        cachedMusic as AVAudioPlayer
        cachedMusic.setNumberOfLoops(if (shouldLoop) NSIntegerMax else 0)
        if (!cachedMusic.isPlaying()) {
            cachedMusic.play()
        }
    }

    override fun isPlaying(music: Any) = (music as AVAudioPlayer).isPlaying()

    override fun pause(music: Any) {
        music as AVAudioPlayer
        if (music.isPlaying()) {
            music.pause()
        }
    }

    // TODO: Works like a pause, not a stop
    override fun stop(music: Any) {
        music as AVAudioPlayer
        if (music.isPlaying()) {
            music.stop()
        }
    }

    override fun dispose(music: Any) = stop(music)

    override fun dispose() = Unit
}