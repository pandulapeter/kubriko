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
 * TODO: Documentation
 * NOTE: Wav files only. To keep compatibility with older Android versions, keep bitrate at a maximum of 48k.
 */
// TODO: Add API to control the volume
sealed class SoundManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "SoundManager",
) {

    abstract fun getLoadingProgress(uris: Collection<String>): Flow<Float>

    abstract fun preload(vararg uris: String)

    abstract fun preload(uris: Collection<String>)

    abstract fun play(uri: String)

    abstract fun unload(uri: String)

    companion object {
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