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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

@Composable
internal actual fun createMusicPlayer(coroutineScope: CoroutineScope) = object : MusicPlayer {

    override suspend fun preload(uri: String) = withContext(Dispatchers.IO) {
        try {
            val resolvedUri = URI(uri)
            val inputStream = if (resolvedUri.isAbsolute) resolvedUri.toURL().openStream() else FileInputStream(resolvedUri.toString())
            DesktopMusicPlayer(inputStream)
        } catch (_: IOException) {
            null
        } catch (_: URISyntaxException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    override suspend fun play(cachedMusic: Any, shouldLoop: Boolean, shouldRestart: Boolean) {
        // Propagate restart flag so the player can rewind the buffered stream before playback.
        (cachedMusic as DesktopMusicPlayer).play(coroutineScope, shouldLoop, shouldRestart)
    }

    override fun isPlaying(cachedMusic: Any) = (cachedMusic as DesktopMusicPlayer).isPlaying

    override fun pause(cachedMusic: Any) {
        (cachedMusic as DesktopMusicPlayer).pause()
    }

    override fun stop(cachedMusic: Any) {
        (cachedMusic as DesktopMusicPlayer).stop()
    }

    override fun setVolume(cachedMusic: Any, leftVolume: Float, rightVolume: Float) {
        // Desktop uses per-frame scaling, so we simply forward the requested gains.
        (cachedMusic as DesktopMusicPlayer).setVolume(leftVolume, rightVolume)
    }

    override fun dispose(cachedMusic: Any) {
        (cachedMusic as DesktopMusicPlayer).dispose()
    }

    override fun dispose() = Unit
}

internal actual val musicPauseDelayOnFocusLoss: Long = 0L
