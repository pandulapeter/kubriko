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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSURL

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun createSoundPlayer(
    maximumSimultaneousStreamsOfTheSameSound: Int,
) = object : SoundPlayer {

    init {
        AVAudioSession.sharedInstance().apply {
            setCategory(AVAudioSessionCategoryPlayback, error = null)
            setActive(true, error = null)
        }
    }

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
        
        // Try to find an available player with a reasonable retry limit
        val maxAttempts = 10
        var attempts = 0
        var wasSoundPlayed = false
        
        while (!wasSoundPlayed && attempts < maxAttempts) {
            val availablePlayer = cachedSound.firstOrNull { !it.playing }
            if (availablePlayer != null) {
                wasSoundPlayed = availablePlayer.play()
            } else {
                // Small delay before retrying
                delay(5)
                attempts++
            }
        }
        
        // Fallback: If still not played after max attempts, force-restart the first player
        if (!wasSoundPlayed) {
            cachedSound.firstOrNull()?.let { player ->
                player.stop()
                player.setCurrentTime(0.0)
                player.play()
            }
        }
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
