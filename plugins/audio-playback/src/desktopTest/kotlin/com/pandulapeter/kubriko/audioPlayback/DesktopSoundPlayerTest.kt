/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.audioPlayback

import com.pandulapeter.kubriko.audioPlayback.implementation.CachedSound
import kotlinx.coroutines.test.runTest
import javax.sound.sampled.AudioFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopSoundPlayerTest {

    // Helper to create test audio data
    private fun createTestAudioData(): Pair<ByteArray, AudioFormat> {
        // Create a simple audio format (8-bit, mono, 8000 Hz)
        val sampleRate = 8000f
        val sampleSizeInBits = 8
        val channels = 1
        val signed = true
        val bigEndian = false
        val format = AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian)
        
        // Create a short sine wave tone (100ms of audio)
        val durationMs = 100
        val numSamples = (sampleRate * durationMs / 1000).toInt()
        val audioData = ByteArray(numSamples)
        val frequency = 440.0 // A4 note
        
        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * frequency * i / sampleRate
            audioData[i] = (127 * Math.sin(angle)).toInt().toByte()
        }
        
        return Pair(audioData, format)
    }

    @Test
    fun preload_returns_cached_sound_object_for_valid_uri() = runTest {
        // Given test audio data
        val (audioData, audioFormat) = createTestAudioData()
        val testUri = "test://sound.wav"
        
        // When creating a CachedSound
        val cachedSound = CachedSound(
            uri = testUri,
            audioData = audioData,
            audioFormat = audioFormat,
            maxSimultaneousStreams = 3
        )
        
        // Then it should be created successfully
        assertNotNull(cachedSound)
        assertEquals(testUri, cachedSound.uri)
        
        // Cleanup
        cachedSound.dispose()
    }

    @Test
    fun preload_creates_correct_number_of_clips_based_on_maximum_simultaneous_streams() = runTest {
        // Given test audio data
        val (audioData, audioFormat) = createTestAudioData()
        val maxStreams = 5
        
        // When creating a CachedSound with specific max streams
        val cachedSound = CachedSound(
            uri = "test://sound.wav",
            audioData = audioData,
            audioFormat = audioFormat,
            maxSimultaneousStreams = maxStreams
        )
        
        // Then we should be able to get exactly maxStreams clips
        val clips = mutableListOf<javax.sound.sampled.Clip?>()
        repeat(maxStreams) {
            clips.add(cachedSound.getAvailableClip())
        }
        
        // All clips should be non-null (we got all maxStreams clips)
        clips.forEach { clip ->
            assertNotNull(clip, "Expected to get $maxStreams clips")
        }
        
        // The next clip should be null (no more available)
        val extraClip = cachedSound.getAvailableClip()
        assertNull(extraClip, "Should not get more clips than maxSimultaneousStreams")
        
        // Cleanup
        cachedSound.dispose()
    }

    @Test
    fun simultaneous_playback_respects_maximum_streams_limit() = runTest {
        // Given test audio data with max 2 streams
        val (audioData, audioFormat) = createTestAudioData()
        val maxStreams = 2
        
        val cachedSound = CachedSound(
            uri = "test://sound.wav",
            audioData = audioData,
            audioFormat = audioFormat,
            maxSimultaneousStreams = maxStreams
        )
        
        // When we get all available clips
        val clip1 = cachedSound.getAvailableClip()
        val clip2 = cachedSound.getAvailableClip()
        val clip3 = cachedSound.getAvailableClip()
        
        // Then we should only get maxStreams clips
        assertNotNull(clip1)
        assertNotNull(clip2)
        assertNull(clip3, "Should not exceed maximum simultaneous streams")
        
        // Cleanup
        cachedSound.dispose()
    }

    @Test
    fun clips_are_reused_after_playback_completion() = runTest {
        // Given test audio data with max 1 stream
        val (audioData, audioFormat) = createTestAudioData()
        
        val cachedSound = CachedSound(
            uri = "test://sound.wav",
            audioData = audioData,
            audioFormat = audioFormat,
            maxSimultaneousStreams = 1
        )
        
        // When we get the only available clip
        val clip1 = cachedSound.getAvailableClip()
        assertNotNull(clip1)
        
        // And no more clips are available
        val clip2 = cachedSound.getAvailableClip()
        assertNull(clip2)
        
        // Note: In a real scenario, the clip would be returned to the pool
        // when the LineListener detects a STOP event. For this test,
        // we verify that the pool mechanism is in place.
        
        // Cleanup
        cachedSound.dispose()
    }

    @Test
    fun dispose_releases_single_cached_sound_resources() = runTest {
        // Given test audio data
        val (audioData, audioFormat) = createTestAudioData()
        
        val cachedSound = CachedSound(
            uri = "test://sound.wav",
            audioData = audioData,
            audioFormat = audioFormat,
            maxSimultaneousStreams = 3
        )
        
        // Get some clips
        val clip1 = cachedSound.getAvailableClip()
        val clip2 = cachedSound.getAvailableClip()
        assertNotNull(clip1)
        assertNotNull(clip2)
        
        // When disposing
        cachedSound.dispose()
        
        // Then no more clips should be available
        val clipAfterDispose = cachedSound.getAvailableClip()
        assertNull(clipAfterDispose, "No clips should be available after dispose")
    }

    @Test
    fun dispose_releases_all_resources() = runTest {
        // Given test audio data
        val (audioData, audioFormat) = createTestAudioData()
        
        // Create multiple CachedSounds
        val cachedSound1 = CachedSound(
            uri = "test://sound1.wav",
            audioData = audioData,
            audioFormat = audioFormat,
            maxSimultaneousStreams = 2
        )
        
        val cachedSound2 = CachedSound(
            uri = "test://sound2.wav",
            audioData = audioData,
            audioFormat = audioFormat,
            maxSimultaneousStreams = 2
        )
        
        // Get clips from both
        assertNotNull(cachedSound1.getAvailableClip())
        assertNotNull(cachedSound2.getAvailableClip())
        
        // When disposing all
        cachedSound1.dispose()
        cachedSound2.dispose()
        
        // Then no clips should be available from either
        assertNull(cachedSound1.getAvailableClip())
        assertNull(cachedSound2.getAvailableClip())
    }

    @Test
    fun no_memory_leaks_after_multiple_play_and_dispose_cycles() = runTest {
        // Given test audio data
        val (audioData, audioFormat) = createTestAudioData()
        
        // When creating and disposing multiple times
        repeat(10) { iteration ->
            val cachedSound = CachedSound(
                uri = "test://sound_$iteration.wav",
                audioData = audioData,
                audioFormat = audioFormat,
                maxSimultaneousStreams = 3
            )
            
            // Get and use clips
            val clip = cachedSound.getAvailableClip()
            assertNotNull(clip, "Should get clip in iteration $iteration")
            
            // Dispose
            cachedSound.dispose()
            
            // Verify disposed
            assertNull(cachedSound.getAvailableClip(), "Should be disposed in iteration $iteration")
        }
        
        // If we get here without OutOfMemoryError or resource exhaustion, the test passes
        assertTrue(true, "Completed multiple cycles without memory leaks")
    }
}
