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
 * Manager responsible for playing short sound effects (SFX).
 *
 * It is optimized for low-latency playback of small audio files.
 * Audio files are identified using Uri. Use the Compose Resources library to provide Uri-s: Res.getUri("file_location"),
 *
 * Note: Only WAV files are supported. To ensure compatibility with older Android versions,
 * keep the bitrate at a maximum of 48k.
 *
 * @param isLoggingEnabled Whether to enable logging for this manager.
 * @param instanceNameForLogging Optional name to use for this instance in log messages.
 */
sealed class SoundManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "SoundManager",
) {

    /**
     * Returns a [Flow] representing the loading progress of the specified [uris].
     *
     * @param uris The identifiers of the sounds to check.
     * @return A flow emitting values between 0.0 and 1.0.
     */
    abstract fun getLoadingProgress(uris: Collection<String>): Flow<Float>

    /**
     * Preloads the specified sound [uris] into memory.
     */
    abstract fun preload(vararg uris: String)

    /**
     * Preloads the specified sound [uris] into memory.
     */
    abstract fun preload(uris: Collection<String>)

    /**
     * Plays the sound associated with the given [uri].
     *
     * If the sound is not yet loaded, it will be loaded automatically.
     */
    abstract fun play(uri: String)

    /**
     * Unloads the specified sound [uri] from memory.
     */
    abstract fun unload(uri: String)

    companion object {
        /**
         * Creates a new instance of [SoundManager].
         *
         * @param maximumSimultaneousStreamsOfTheSameSound The maximum number of times the same sound can be played simultaneously.
         * @param isLoggingEnabled Whether to enable internal logging.
         * @param instanceNameForLogging An optional name to identify this instance in logs.
         */
        fun newInstance(
            maximumSimultaneousStreamsOfTheSameSound: Int = 5,
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): SoundManager = SoundManagerImpl(
            maximumSimultaneousStreamsOfTheSameSound = maximumSimultaneousStreamsOfTheSameSound,
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}