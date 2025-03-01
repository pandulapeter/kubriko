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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.audioPlayback.implementation.SoundPlayer
import com.pandulapeter.kubriko.audioPlayback.implementation.createSoundPlayer
import com.pandulapeter.kubriko.logger.Logger
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class SoundManagerImpl(
    private val maximumSimultaneousStreamsOfTheSameSound: Int,
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : SoundManager(isLoggingEnabled, instanceNameForLogging) {
    private val cache = MutableStateFlow(persistentMapOf<String, Any?>())
    private var soundPlayer: SoundPlayer? = null
    private val stateManager by manager<StateManager>()

    @Composable
    override fun Composable(windowInsets: WindowInsets) {
        if (soundPlayer == null) {
            soundPlayer = createSoundPlayer(maximumSimultaneousStreamsOfTheSameSound).also { soundPlayer ->
                scope.launch {
                    cache.value.keys.forEach { uri ->
                        soundPlayer.preload(uri)?.let { sound -> addToCache(uri, sound) }
                    }
                }
            }
        }
    }

    override fun getLoadingProgress(uris: Collection<String>) = if (uris.isEmpty()) flowOf(1f) else cache.map { cache ->
        cache.filter { (key, _) -> key in uris }.count { (_, value) -> value != null }.toFloat() / uris.size
    }.distinctUntilChanged()

    override fun preload(vararg uris: String) = preload(uris.toSet())

    override fun preload(uris: Collection<String>) {
        uris.forEach { uri ->
            if (!cache.value.contains(uri)) {
                addToCache(uri, null)
                scope.launch {
                    soundPlayer?.preload(uri)?.let { sound -> addToCache(uri, sound) }
                }
            }
        }
    }

    private fun addToCache(uri: String, sound: Any?) {
        if (sound == null) {
            log(
                message = "Preloading ${uri}...",
                importance = Logger.Importance.LOW,
            )
        } else {
            log(
                message = "${uri.substringAfterLast('/')} preloaded.",
                importance = Logger.Importance.MEDIUM,
            )
        }
        cache.update { it.put(uri, sound) }
    }

    override fun play(uri: String) {
        scope.launch {
            if (stateManager.isFocused.value) {
                soundPlayer?.let { soundPlayer ->
                    val cachedSound = cache.value[uri]
                    if (cachedSound == null) {
                        soundPlayer.preload(uri)?.let { sound ->
                            addToCache(uri, sound)
                            soundPlayer.play(sound)
                        }
                    } else {
                        soundPlayer.play(cachedSound)
                    }
                }
            }
        }
    }

    override fun unload(uri: String) {
        scope.launch {
            cache.value[uri]?.let { sound -> soundPlayer?.dispose(sound) }
            cache.update { cache.value.remove(uri) }
        }
    }

    override fun onDispose() {
        soundPlayer?.let { soundPlayer ->
            cache.value.values.filterNotNull().forEach { sound -> soundPlayer.dispose(sound) }
            soundPlayer.dispose()
        }
        cache.update { persistentMapOf() }
        soundPlayer = null
    }
}