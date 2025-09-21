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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSURL

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun createSoundPlayer(
    maximumSimultaneousStreamsOfTheSameSound: Int,
) = object : SoundPlayer {

    override suspend fun preload(uri: String) = withContext(Dispatchers.Default) {
        buildList {
            repeat(maximumSimultaneousStreamsOfTheSameSound) {
                add(
                    AVAudioPlayer(NSURL.URLWithString(URLString = uri)!!, error = null).apply {
                        prepareToPlay()
                    }
                )
            }
        }
    }

    override suspend fun play(cachedSound: Any) = withContext(Dispatchers.Default) {
        cachedSound as List<AVAudioPlayer>
        var wasSoundPlayed: Boolean
        do {
            wasSoundPlayed = cachedSound.firstOrNull { !it.playing }?.play() == true
        } while (!wasSoundPlayed)
    }

    override fun dispose(cachedSound: Any) {
        cachedSound as List<AVAudioPlayer>
        cachedSound.forEach {
            if (it.isPlaying()) {
                it.stop()
            }
        }
    }

    override fun dispose() = Unit
}