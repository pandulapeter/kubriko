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

import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.flow.Flow

/**
 * Manager responsible for playing background music.
 *
 * It is optimized for streaming larger audio files from disk.
 * Audio files are identified using Uri. Use the Compose Resources library to provide Uri-s: Res.getUri("file_location"),
 *
 * Note: MP3 files at upd 320 kbps are recommended. Use other formats at your own risk.
 *
 * @param isLoggingEnabled Whether to enable logging for this manager.
 * @param instanceNameForLogging Optional name to use for this instance in log messages.
 */
sealed class MusicManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "MusicManager",
) {
    /**
     * Clears the audio cache without disposing the music manager.
     */
    abstract fun unloadAll()

    /**
     * Returns a [Flow] representing the loading progress of the specified [uri].
     *
     * @param uri The identifier of the music file to check.
     * @return A flow emitting values between 0.0 and 1.0.
     */
    abstract fun getLoadingProgress(uri: String): Flow<Float>

    /**
     * Returns a [Flow] representing the cumulative loading progress of the specified [uris].
     *
     * @param uris The identifiers of the music files to check.
     * @return A flow emitting values between 0.0 and 1.0.
     */
    abstract fun getLoadingProgress(uris: Collection<String>): Flow<Float>

    /**
     * Preloads the specified music [uris] into memory.
     */
    abstract fun preload(vararg uris: String)

    /**
     * Preloads the specified music [uris] into memory.
     */
    abstract fun preload(uris: Collection<String>)

    /**
     * Returns whether the music associated with the given [uri] is currently playing.
     */
    abstract fun isPlaying(uri: String): Boolean

    /**
     * Starts playback of the music associated with the given [uri].
     *
     * @param uri The identifier of the music file.
     * @param shouldLoop Whether the music should automatically restart when it finishes.
     * @param shouldRestart Whether to restart the music from the beginning if it is already playing.
     */
    abstract fun play(uri: String, shouldLoop: Boolean = true, shouldRestart: Boolean = false)

    /**
     * Pauses playback of the music associated with the given [uri].
     */
    abstract fun pause(uri: String)

    /**
     * Stops playback of the music associated with the given [uri] and resets its position.
     */
    abstract fun stop(uri: String)

    /**
     * Unloads the specified music [uri] from memory.
     */
    abstract fun unload(uri: String)

    /**
     * Sets the volume for the music associated with the given [uri].
     *
     * @param uri The identifier of the music file.
     * @param leftVolume The volume for the left channel (0.0 to 1.0).
     * @param rightVolume The volume for the right channel (0.0 to 1.0).
     */
    abstract fun setVolume(uri: String, leftVolume: Float, rightVolume: Float)

    /**
     * Sets the default volume for all new music playbacks.
     *
     * @param leftVolume The default volume for the left channel (0.0 to 1.0).
     * @param rightVolume The default volume for the right channel (0.0 to 1.0).
     */
    abstract fun setDefaultVolume(leftVolume: Float, rightVolume: Float)

    /**
     * Returns the current volume for the music associated with the given [uri].
     *
     * @return A [Pair] containing the left and right channel volumes.
     */
    abstract fun getVolume(uri: String): Pair<Float, Float>

    companion object {
        /**
         * Creates a new instance of [MusicManager].
         *
         * @param isLoggingEnabled Whether to enable internal logging.
         * @param instanceNameForLogging An optional name to identify this instance in logs.
         */
        fun newInstance(
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): MusicManager = MusicManagerImpl(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}
