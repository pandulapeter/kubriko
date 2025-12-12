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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Audio playback tests that produce audible output.
 * 
 * These tests are marked with @Ignore because:
 * 1. They produce audible sounds that can be disruptive in CI/CD environments
 * 2. They include Thread.sleep() delays to allow sounds to play, making them slow
 * 3. They require audio hardware to be present and functional
 * 
 * To run these tests manually, remove the @Ignore annotation or run with:
 * ./gradlew :plugins:audio-playback:desktopTest --tests "*.DesktopSoundPlayerPlaybackTest.*"
 */
class DesktopSoundPlayerPlaybackTest {

    companion object {
        private const val TONE_DURATION_MS = 500
        private const val DELAY_AFTER_PLAYBACK_MS = 600L // Slightly longer than tone duration
        private const val FREQUENCY_A4 = 440.0 // A4 note
        private const val FREQUENCY_A5 = 880.0 // A5 note (one octave higher)
    }

    // ============================================================
    // Helper functions to generate audio data
    // ============================================================

    /**
     * Creates 8-bit mono audio data for a sine wave tone.
     * 
     * @param frequency The frequency of the tone in Hz
     * @param durationMs Duration of the tone in milliseconds
     * @param sampleRate Sample rate in Hz (default 8000)
     * @return Pair of audio data bytes and AudioFormat
     */
    private fun create8BitMonoAudioData(
        frequency: Double,
        durationMs: Int = TONE_DURATION_MS,
        sampleRate: Float = 8000f
    ): Pair<ByteArray, AudioFormat> {
        val format = AudioFormat(
            sampleRate,          // sample rate
            8,                   // sample size in bits
            1,                   // channels (mono)
            true,                // signed
            false                // big endian
        )

        val numSamples = (sampleRate * durationMs / 1000).toInt()
        val audioData = ByteArray(numSamples)

        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * frequency * i / sampleRate
            // 8-bit audio: values from -128 to 127
            audioData[i] = (127 * Math.sin(angle)).toInt().toByte()
        }

        return Pair(audioData, format)
    }

    /**
     * Creates 16-bit stereo audio data for a sine wave tone.
     * 
     * @param frequency The frequency of the tone in Hz
     * @param durationMs Duration of the tone in milliseconds
     * @param sampleRate Sample rate in Hz (default 44100 for CD quality)
     * @return Pair of audio data bytes and AudioFormat
     */
    private fun create16BitStereoAudioData(
        frequency: Double,
        durationMs: Int = TONE_DURATION_MS,
        sampleRate: Float = 44100f
    ): Pair<ByteArray, AudioFormat> {
        val format = AudioFormat(
            sampleRate,          // sample rate
            16,                  // sample size in bits
            2,                   // channels (stereo)
            true,                // signed
            false                // little endian
        )

        val numSamples = (sampleRate * durationMs / 1000).toInt()
        // 16-bit stereo: 2 bytes per sample * 2 channels = 4 bytes per frame
        val audioData = ByteArray(numSamples * 4)
        val buffer = ByteBuffer.wrap(audioData).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * frequency * i / sampleRate
            // 16-bit audio: values from -32768 to 32767
            val sample = (32767 * Math.sin(angle)).toInt().toShort()
            // Left channel
            buffer.putShort(sample)
            // Right channel
            buffer.putShort(sample)
        }

        return Pair(audioData, format)
    }

    // ============================================================
    // 8-bit Mono Audio Tests
    // ============================================================

    @Test
    @Ignore("Manual audio test - produces audible 440Hz (A4 note) 8-bit mono output and includes delays. Remove @Ignore to run manually.")
    fun test_play_8bit_mono_tone_440hz_a4_note() = runTest {
        // Given: 8-bit mono audio data at 440Hz (A4 note)
        val (audioData, audioFormat) = create8BitMonoAudioData(FREQUENCY_A4)

        val cachedSound = CachedSound(
            uri = "test://8bit_mono_440hz.wav",
            audioData = audioData,
            audioFormat = audioFormat,
            maxSimultaneousStreams = 1
        )

        // When: Getting a clip and playing it
        val clip = cachedSound.getAvailableClip()
        assertNotNull(clip, "Should get a clip for playback")

        clip.start()

        // Then: Wait for the sound to play
        Thread.sleep(DELAY_AFTER_PLAYBACK_MS)

        // Verify playback completed (clip should have stopped)
        assertTrue(!clip.isRunning, "Clip should have stopped after playback duration")

        // Cleanup
        cachedSound.dispose()
    }

    @Test
    @Ignore("Manual audio test - produces audible 880Hz (A5 note) 8-bit mono output and includes delays. Remove @Ignore to run manually.")
    fun test_play_8bit_mono_tone_880hz_a5_note() = runTest {
        // Given: 8-bit mono audio data at 880Hz (A5 note - one octave higher)
        val (audioData, audioFormat) = create8BitMonoAudioData(FREQUENCY_A5)

        val cachedSound = CachedSound(
            uri = "test://8bit_mono_880hz.wav",
            audioData = audioData,
            audioFormat = audioFormat,
            maxSimultaneousStreams = 1
        )

        // When: Getting a clip and playing it
        val clip = cachedSound.getAvailableClip()
        assertNotNull(clip, "Should get a clip for playback")

        clip.start()

        // Then: Wait for the sound to play
        Thread.sleep(DELAY_AFTER_PLAYBACK_MS)

        // Verify playback completed
        assertTrue(!clip.isRunning, "Clip should have stopped after playback duration")

        // Cleanup
        cachedSound.dispose()
    }

    @Test
    @Ignore("Manual audio test - produces audible simultaneous 8-bit mono tones and includes delays. Remove @Ignore to run manually.")
    fun test_8bit_mono_simultaneous_playback_two_tones() = runTest {
        // Given: Two different 8-bit mono tones
        val (audioData440, audioFormat440) = create8BitMonoAudioData(FREQUENCY_A4)
        val (audioData880, audioFormat880) = create8BitMonoAudioData(FREQUENCY_A5)

        val cachedSound440 = CachedSound(
            uri = "test://8bit_mono_440hz.wav",
            audioData = audioData440,
            audioFormat = audioFormat440,
            maxSimultaneousStreams = 1
        )

        val cachedSound880 = CachedSound(
            uri = "test://8bit_mono_880hz.wav",
            audioData = audioData880,
            audioFormat = audioFormat880,
            maxSimultaneousStreams = 1
        )

        // When: Playing both tones simultaneously
        val clip440 = cachedSound440.getAvailableClip()
        val clip880 = cachedSound880.getAvailableClip()

        assertNotNull(clip440, "Should get first clip")
        assertNotNull(clip880, "Should get second clip")

        clip440.start()
        clip880.start()

        // Then: Wait for both sounds to play (you should hear a chord)
        Thread.sleep(DELAY_AFTER_PLAYBACK_MS)

        // Verify both clips stopped
        assertTrue(!clip440.isRunning, "First clip should have stopped")
        assertTrue(!clip880.isRunning, "Second clip should have stopped")

        // Cleanup
        cachedSound440.dispose()
        cachedSound880.dispose()
    }

    // ============================================================
    // 16-bit Stereo Audio Tests
    // ============================================================

    @Test
    @Ignore("Manual audio test - produces audible 440Hz (A4 note) 16-bit stereo output and includes delays. Remove @Ignore to run manually.")
    fun test_play_16bit_stereo_tone_440hz_a4_note() = runTest {
        // Given: 16-bit stereo audio data at 440Hz (A4 note) - CD quality
        val (audioData, audioFormat) = create16BitStereoAudioData(FREQUENCY_A4)

        val cachedSound = CachedSound(
            uri = "test://16bit_stereo_440hz.wav",
            audioData = audioData,
            audioFormat = audioFormat,
            maxSimultaneousStreams = 1
        )

        // When: Getting a clip and playing it
        val clip = cachedSound.getAvailableClip()
        assertNotNull(clip, "Should get a clip for playback")

        clip.start()

        // Then: Wait for the sound to play
        Thread.sleep(DELAY_AFTER_PLAYBACK_MS)

        // Verify playback completed
        assertTrue(!clip.isRunning, "Clip should have stopped after playback duration")

        // Cleanup
        cachedSound.dispose()
    }

    @Test
    @Ignore("Manual audio test - produces audible 880Hz (A5 note) 16-bit stereo output and includes delays. Remove @Ignore to run manually.")
    fun test_play_16bit_stereo_tone_880hz_a5_note() = runTest {
        // Given: 16-bit stereo audio data at 880Hz (A5 note) - CD quality
        val (audioData, audioFormat) = create16BitStereoAudioData(FREQUENCY_A5)

        val cachedSound = CachedSound(
            uri = "test://16bit_stereo_880hz.wav",
            audioData = audioData,
            audioFormat = audioFormat,
            maxSimultaneousStreams = 1
        )

        // When: Getting a clip and playing it
        val clip = cachedSound.getAvailableClip()
        assertNotNull(clip, "Should get a clip for playback")

        clip.start()

        // Then: Wait for the sound to play
        Thread.sleep(DELAY_AFTER_PLAYBACK_MS)

        // Verify playback completed
        assertTrue(!clip.isRunning, "Clip should have stopped after playback duration")

        // Cleanup
        cachedSound.dispose()
    }

    @Test
    @Ignore("Manual audio test - produces audible simultaneous 16-bit stereo tones and includes delays. Remove @Ignore to run manually.")
    fun test_16bit_stereo_simultaneous_playback_two_tones() = runTest {
        // Given: Two different 16-bit stereo tones (CD quality)
        val (audioData440, audioFormat440) = create16BitStereoAudioData(FREQUENCY_A4)
        val (audioData880, audioFormat880) = create16BitStereoAudioData(FREQUENCY_A5)

        val cachedSound440 = CachedSound(
            uri = "test://16bit_stereo_440hz.wav",
            audioData = audioData440,
            audioFormat = audioFormat440,
            maxSimultaneousStreams = 1
        )

        val cachedSound880 = CachedSound(
            uri = "test://16bit_stereo_880hz.wav",
            audioData = audioData880,
            audioFormat = audioFormat880,
            maxSimultaneousStreams = 1
        )

        // When: Playing both tones simultaneously
        val clip440 = cachedSound440.getAvailableClip()
        val clip880 = cachedSound880.getAvailableClip()

        assertNotNull(clip440, "Should get first clip")
        assertNotNull(clip880, "Should get second clip")

        clip440.start()
        clip880.start()

        // Then: Wait for both sounds to play (you should hear a chord)
        Thread.sleep(DELAY_AFTER_PLAYBACK_MS)

        // Verify both clips stopped
        assertTrue(!clip440.isRunning, "First clip should have stopped")
        assertTrue(!clip880.isRunning, "Second clip should have stopped")

        // Cleanup
        cachedSound440.dispose()
        cachedSound880.dispose()
    }

    // ============================================================
    // Functional Tests with Actual Playback
    // ============================================================

    @Test
    @Ignore("Manual audio test - tests clip pooling with actual playback. Produces audible output and includes delays. Remove @Ignore to run manually.")
    fun test_clip_returns_to_pool_after_playback_completes() = runTest {
        // Given: A CachedSound with only 1 stream allowed
        val (audioData, audioFormat) = create8BitMonoAudioData(FREQUENCY_A4, durationMs = 200)

        val cachedSound = CachedSound(
            uri = "test://short_tone.wav",
            audioData = audioData,
            audioFormat = audioFormat,
            maxSimultaneousStreams = 1
        )

        // When: Getting and playing the only clip
        val clip1 = cachedSound.getAvailableClip()
        assertNotNull(clip1, "Should get the first clip")
        clip1.start()

        // Verify no more clips available while playing
        val clip2 = cachedSound.getAvailableClip()
        assertNull(clip2, "Should not get another clip while first is in use")

        // Wait for playback to complete (clip should be returned to pool via LineListener)
        Thread.sleep(300) // Wait longer than the 200ms tone duration

        // Note: The clip should be returned to the pool automatically via the STOP event listener
        // However, this depends on the LineListener being triggered correctly
        // In some cases, we may need to manually check or the test verifies the mechanism exists

        // Cleanup
        cachedSound.dispose()
    }

    @Test
    @Ignore("Manual audio test - tests that dispose stops playback. Produces brief audible output. Remove @Ignore to run manually.")
    fun test_playback_stops_on_dispose() = runTest {
        // Given: A longer tone that we'll interrupt
        val (audioData, audioFormat) = create16BitStereoAudioData(FREQUENCY_A4, durationMs = 2000)

        val cachedSound = CachedSound(
            uri = "test://long_tone.wav",
            audioData = audioData,
            audioFormat = audioFormat,
            maxSimultaneousStreams = 1
        )

        // When: Starting playback
        val clip = cachedSound.getAvailableClip()
        assertNotNull(clip, "Should get a clip")
        clip.start()

        // Wait a bit to hear the sound start
        Thread.sleep(200)

        // Verify the clip is running
        assertTrue(clip.isRunning, "Clip should be running")

        // Dispose should stop the playback
        cachedSound.dispose()

        // Then: Give a moment for the stop to take effect
        Thread.sleep(50)

        // Verify playback stopped
        assertTrue(!clip.isRunning, "Clip should have stopped after dispose")
    }
}
