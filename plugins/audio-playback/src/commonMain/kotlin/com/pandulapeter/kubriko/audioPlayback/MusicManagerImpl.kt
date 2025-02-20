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
import com.pandulapeter.kubriko.audioPlayback.implementation.MusicPlayer
import com.pandulapeter.kubriko.audioPlayback.implementation.createMusicPlayer
import com.pandulapeter.kubriko.audioPlayback.implementation.onManagerDisposed
import com.pandulapeter.kubriko.logger.Logger
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class MusicManagerImpl(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : MusicManager(isLoggingEnabled, instanceNameForLogging) {
    private val cache = MutableStateFlow(persistentMapOf<String, Any?>())
    private var musicPlayer: MusicPlayer? = null
    private val stateManager by manager<StateManager>()

    @Composable
    override fun Composable(windowInsets: WindowInsets) {
        if (musicPlayer == null) {
            musicPlayer = createMusicPlayer(scope).also { soundPlayer ->
                scope.launch {
                    cache.value.keys.forEach { uri ->
                        soundPlayer.preload(uri)?.let { music -> addToCache(uri, music) }
                    }
                }
            }
            stateManager.isFocused
                .filterNot { it }
                .onEach { cache.value.keys.forEach(::pause) }
                .launchIn(scope)
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
                    musicPlayer?.preload(uri)?.let { music -> addToCache(uri, music) }
                }
            }
        }
    }

    override fun isPlaying(uri: String) = cache.value[uri].let { music ->
        music != null && musicPlayer?.isPlaying(music) == true
    }

    private fun addToCache(uri: String, music: Any?) {
        if (music == null) {
            log(
                message = "Preloading ${uri.substringAfterLast('/')}...",
                importance = Logger.Importance.LOW,
            )
        } else {
            log(
                message = "${uri.substringAfterLast('/')} preloaded.",
                importance = Logger.Importance.MEDIUM,
            )
        }
        cache.update { it.put(uri, music) }
    }

    override fun play(uri: String, shouldLoop: Boolean) {
        musicPlayer?.let { musicPlayer ->
            if (!isPlaying(uri)) {
                val cachedSound = cache.value[uri]
                scope.launch {
                    if (cachedSound == null) {
                        musicPlayer.preload(uri)?.let { music ->
                            addToCache(uri, music)
                            if (stateManager.isFocused.value && !isPlaying(uri)) {
                                musicPlayer.play(music, shouldLoop)
                            }
                        }
                    } else {
                        if (stateManager.isFocused.value && !isPlaying(uri)) {
                            musicPlayer.play(cachedSound, shouldLoop)
                        }
                    }
                }
            }
        }
    }

    override fun pause(uri: String) {
        if (isPlaying(uri)) {
            cache.value[uri]?.let { music -> musicPlayer?.pause(music) }
        }
    }

    override fun stop(uri: String) {
        if (isPlaying(uri)) {
            scope.launch {
                cache.value[uri]?.let { music -> musicPlayer?.stop(music) }
            }
        }
    }

    override fun unload(uri: String) {
        scope.launch {
            cache.value[uri]?.let { music -> musicPlayer?.dispose(music) }
            cache.update { it.remove(uri) }
        }
    }

    override fun onDispose() {
        musicPlayer?.onManagerDisposed(cache.value)
        musicPlayer = null
        cache.update { persistentMapOf() }
    }
}