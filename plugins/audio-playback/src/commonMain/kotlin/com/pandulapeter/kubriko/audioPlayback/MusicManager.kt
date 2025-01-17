/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
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
 */
// TODO: Add API to control the volume
sealed class MusicManager(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : Manager(
    isLoggingEnabled = isLoggingEnabled,
    instanceNameForLogging = instanceNameForLogging,
    classNameForLogging = "MusicManager",
) {

    abstract fun getLoadingProgress(uris: Collection<String>): Flow<Float>

    abstract fun preload(vararg uris: String)

    abstract fun preload(uris: Collection<String>)

    abstract fun isPlaying(uri: String): Boolean

    abstract fun play(uri: String, shouldLoop: Boolean = true)

    abstract fun pause(uri: String)

    abstract fun stop(uri: String)

    abstract fun unload(uri: String)

    companion object {
        fun newInstance(
            isLoggingEnabled: Boolean = false,
            instanceNameForLogging: String? = null,
        ): MusicManager = MusicManagerImpl(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = instanceNameForLogging,
        )
    }
}