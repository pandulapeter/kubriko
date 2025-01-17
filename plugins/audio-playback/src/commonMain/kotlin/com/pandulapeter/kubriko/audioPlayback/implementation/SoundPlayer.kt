/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable

internal interface SoundPlayer {

    suspend fun preload(uri: String) : Any?

    suspend fun play(cachedSound: Any)

    fun dispose(cachedSound: Any)

    fun dispose()
}

@Composable
internal expect fun createSoundPlayer(maximumSimultaneousStreamsOfTheSameSound: Int): SoundPlayer