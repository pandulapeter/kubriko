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

import androidx.compose.runtime.Composable
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.net.URI
import javax.sound.sampled.AudioSystem

@OptIn(DelicateCoroutinesApi::class)
@Composable
internal actual fun createSoundPlayer(
    maximumSimultaneousStreamsOfTheSameSound: Int,
) = object : SoundPlayer {

    override suspend fun preload(uri: String): Any? = withContext(Dispatchers.IO) {
        try {
            val inputStream = URI(uri).let { resolvedUri ->
                if (resolvedUri.isAbsolute) {
                    resolvedUri.toURL().openStream()
                } else {
                    FileInputStream(resolvedUri.toString())
                }
            }

            inputStream.use {
                AudioSystem.getAudioInputStream(BufferedInputStream(inputStream)).use { audioInputStream ->
                    // Read all audio data into memory
                    val audioData = ByteArrayOutputStream().use { output ->
                        audioInputStream.copyTo(output)
                        output.toByteArray()
                    }
                    CachedSound.create(
                        audioData = audioData,
                        audioFormat = audioInputStream.format,
                        maxSimultaneousStreams = maximumSimultaneousStreamsOfTheSameSound,
                    )
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun play(cachedSound: Any) = withContext(Dispatchers.IO) {
        val sound = cachedSound as CachedSound
        val clip = sound.getAvailableClip()
        clip?.start()
        Unit
    }

    override fun dispose(cachedSound: Any) {
        GlobalScope.launch(Dispatchers.IO) {
            (cachedSound as CachedSound).dispose()
        }
    }

    override fun dispose() = Unit
}
