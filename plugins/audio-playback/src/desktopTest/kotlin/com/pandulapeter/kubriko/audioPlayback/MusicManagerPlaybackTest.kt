/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.audioPlayback

import com.pandulapeter.kubriko.audioPlayback.implementation.MusicPlayer
import com.pandulapeter.kubriko.testFixtures.ManualKubriko
import com.pandulapeter.kubriko.testFixtures.newManualKubriko
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MusicManagerPlaybackTest {

    private val musicPlayer = FakeMusicPlayer()
    private val musicManager = MusicManagerImpl(
        isLoggingEnabled = false,
        instanceNameForLogging = null,
        initialMusicPlayer = musicPlayer,
    )
    private val kubriko: ManualKubriko = newManualKubriko(musicManager)

    @AfterTest
    fun tearDown() = kubriko.dispose()

    @Test
    fun stopRewindsAPausedTrack() {
        musicManager.play(URI)
        kubriko.tickUntil { musicManager.isPlaying(URI) }
        musicManager.pause(URI)

        musicManager.stop(URI)

        kubriko.tickUntil { musicPlayer.stopCount(URI) == 1 }
    }

    @Test
    fun stopReachesALoadedTrackThatNeverPlayed() {
        musicManager.preload(URI)
        kubriko.tickUntil { runBlocking { musicManager.getLoadingProgress(URI).first() } == 1f }

        musicManager.stop(URI)

        kubriko.tickUntil { musicPlayer.stopCount(URI) == 1 }
    }

    @Test
    fun stopOfAnUnloadedTrackDoesNothing() {
        musicManager.stop(OTHER_URI)
        kubriko.tick(count = 5)

        assertEquals(0, musicPlayer.stopCount(OTHER_URI))
        assertTrue(musicPlayer.preloadedUris.isEmpty())
    }

    private class FakeMusicPlayer : MusicPlayer {
        private class Track {
            @Volatile
            var isPlaying = false
            val stopCount = AtomicInteger()
        }

        private val tracks = ConcurrentHashMap<String, Track>()
        val preloadedUris: Set<String> get() = tracks.keys

        fun stopCount(uri: String) = tracks[uri]?.stopCount?.get() ?: 0

        override suspend fun preload(uri: String): Any = Track().also { tracks[uri] = it }

        override suspend fun play(cachedMusic: Any, shouldLoop: Boolean, shouldRestart: Boolean) {
            (cachedMusic as Track).isPlaying = true
        }

        override fun isPlaying(cachedMusic: Any) = (cachedMusic as Track).isPlaying

        override fun pause(cachedMusic: Any) {
            (cachedMusic as Track).isPlaying = false
        }

        override fun stop(cachedMusic: Any) {
            cachedMusic as Track
            cachedMusic.isPlaying = false
            cachedMusic.stopCount.incrementAndGet()
        }

        override fun setVolume(cachedMusic: Any, leftVolume: Float, rightVolume: Float) = Unit

        override fun dispose(cachedMusic: Any) = Unit

        override fun dispose() = Unit
    }

    private companion object {
        const val URI = "files/music/theme.mp3"
        const val OTHER_URI = "files/music/other.mp3"
    }
}
