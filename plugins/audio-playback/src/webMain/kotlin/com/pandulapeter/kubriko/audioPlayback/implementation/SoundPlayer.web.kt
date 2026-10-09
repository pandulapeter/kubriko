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
import kotlinx.browser.window
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import kotlinx.coroutines.withContext
import org.w3c.dom.HTMLAudioElement
import org.w3c.dom.url.URL

@Composable
internal actual fun createSoundPlayer(
    maximumSimultaneousStreamsOfTheSameSound: Int,
) = object : SoundPlayer {

    /**
     * The file is fetched once and shared by the element pool through an object URL. Waiting for the elements to
     * load instead would hang on iOS Safari, which loads no media before a user gesture.
     */
    override suspend fun preload(uri: String) = withContext(Dispatchers.Default) {
        try {
            val response = window.fetch(uri).await()
            if (!response.ok) {
                null
            } else {
                val objectUrl = URL.createObjectURL(response.blob().await())
                WebCachedSound(
                    objectUrl = objectUrl,
                    elements = List(maximumSimultaneousStreamsOfTheSameSound) {
                        (document.createElement("audio") as HTMLAudioElement).apply {
                            src = objectUrl
                        }
                    },
                )
            }
        } catch (exception: Throwable) {
            if (exception is CancellationException) throw exception
            null
        }
    }

    override suspend fun play(cachedSound: Any) {
        cachedSound as WebCachedSound
        withContext(Dispatchers.Default) {
            // The browser rejects plays before the first user gesture; the element stays paused and is reused.
            cachedSound.elements.firstOrNull { it.paused }?.play()?.catch { null }
        }
    }

    override fun dispose(cachedSound: Any) {
        cachedSound as WebCachedSound
        cachedSound.elements.forEach {
            if (!it.paused) {
                it.pause()
            }
            it.src = ""
            it.remove()
        }
        URL.revokeObjectURL(cachedSound.objectUrl)
    }

    override fun dispose() = Unit
}

private class WebCachedSound(
    val objectUrl: String,
    val elements: List<HTMLAudioElement>,
)
