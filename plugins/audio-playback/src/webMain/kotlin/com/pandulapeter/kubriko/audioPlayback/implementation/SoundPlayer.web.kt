/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
@file:OptIn(ExperimentalWasmJsInterop::class)

package com.pandulapeter.kubriko.audioPlayback.implementation

import androidx.compose.runtime.Composable
import kotlinx.browser.document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.HTMLAudioElement

@Suppress("UNCHECKED_CAST")
@Composable
internal actual fun createSoundPlayer(
    maximumSimultaneousStreamsOfTheSameSound: Int,
) = object : SoundPlayer {

    override suspend fun preload(uri: String) = withContext(Dispatchers.Default) {
        buildList {
            repeat(maximumSimultaneousStreamsOfTheSameSound) {
                add(
                    (document.createElement("audio") as HTMLAudioElement).apply {
                        src = uri
                    }
                )
            }
        }
    }

    override suspend fun play(cachedSound: Any) {
        cachedSound as List<HTMLAudioElement>
        withContext(Dispatchers.Default) {
            cachedSound.firstOrNull { it.paused }?.play()
        }
    }

    override fun dispose(cachedSound: Any) {
        cachedSound as List<HTMLAudioElement>
        cachedSound.forEach {
            if (!it.paused) {
                it.pause()
            }
            it.src = ""
            it.remove()
        }
    }

    override fun dispose() = Unit
}