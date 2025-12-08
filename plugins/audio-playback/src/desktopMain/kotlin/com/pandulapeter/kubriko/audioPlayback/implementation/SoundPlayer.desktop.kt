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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.net.URI
import javax.sound.sampled.AudioSystem

@Composable
internal actual fun createSoundPlayer(
    maximumSimultaneousStreamsOfTheSameSound: Int,
) = object : SoundPlayer {

    private val cachedSounds = mutableMapOf<String, CachedSound>()

    override suspend fun preload(uri: String): Any? = withContext(Dispatchers.IO) {
        // Check if already cached
        cachedSounds[uri]?.let { return@withContext it }

        try {
            val inputStream = URI(uri).let { resolvedUri ->
                if (resolvedUri.isAbsolute) {
                    resolvedUri.toURL().openStream()
                } else {
                    FileInputStream(resolvedUri.toString())
                }
            }

            val bufferedStream = BufferedInputStream(inputStream)
            val audioInputStream = AudioSystem.getAudioInputStream(bufferedStream)

            // Read all audio data into memory
            val audioData = ByteArrayOutputStream().use { output ->
                audioInputStream.copyTo(output)
                output.toByteArray()
            }

            val cachedSound = CachedSound(
                uri = uri,
                audioData = audioData,
                audioFormat = audioInputStream.format,
                maxSimultaneousStreams = maximumSimultaneousStreamsOfTheSameSound
            )

            cachedSounds[uri] = cachedSound
            audioInputStream.close()
            inputStream.close()

            cachedSound
        } catch (e: Exception) {
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
        val sound = cachedSound as CachedSound
        cachedSounds.remove(sound.uri)
        sound.dispose()
    }

    override fun dispose() {
        cachedSounds.values.forEach { it.dispose() }
        cachedSounds.clear()
    }
}
