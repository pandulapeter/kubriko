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
import java.io.FileInputStream
import java.net.URI
import javax.sound.sampled.AudioSystem

@Composable
internal actual fun createSoundPlayer(
    maximumSimultaneousStreamsOfTheSameSound: Int, // TODO: On Desktop, due to the issue below, this limit is not applied
) = object : SoundPlayer {

    // Couldn't figure out how to re-use cached Clips, so preloading on desktop is not supported
    override suspend fun preload(uri: String) = uri

    override suspend fun play(cachedSound: Any) = withContext(Dispatchers.IO) {
        val uri = cachedSound as String
        val clip = AudioSystem.getClip()
        val inputStream = URI(uri).let { resolvedUri ->
            if (resolvedUri.isAbsolute) {
                resolvedUri.toURL().openStream()
            } else {
                FileInputStream(resolvedUri.toString())
            }
        }
        clip.open(AudioSystem.getAudioInputStream(BufferedInputStream(inputStream)))
        clip.start()
    }

    // TODO: Stop the Clip
    override fun dispose(cachedSound: Any) = Unit

    override fun dispose() = Unit
}