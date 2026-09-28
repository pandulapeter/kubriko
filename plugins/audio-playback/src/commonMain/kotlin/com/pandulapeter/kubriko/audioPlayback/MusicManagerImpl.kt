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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.audioPlayback.implementation.AudioCache
import com.pandulapeter.kubriko.audioPlayback.implementation.MusicPlayer
import com.pandulapeter.kubriko.audioPlayback.implementation.createMusicPlayer
import com.pandulapeter.kubriko.audioPlayback.implementation.musicPauseDelayOnFocusLoss
import com.pandulapeter.kubriko.audioPlayback.implementation.onManagerDisposed
import com.pandulapeter.kubriko.logger.Logger
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal class MusicManagerImpl(
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : MusicManager(isLoggingEnabled, instanceNameForLogging) {
    private val audioCache = AudioCache()
    private var musicPlayer: MusicPlayer? = null
    private val stateManager by manager<StateManager>()
    private val volumeConfig = MutableStateFlow(persistentMapOf<String, Pair<Float, Float>>())
    private var defaultVolume: Pair<Float, Float> = Pair(1.0f, 1.0f)

    @OptIn(FlowPreview::class)
    @Composable
    override fun Composable(windowInsets: WindowInsets) {
        if (musicPlayer == null) {
            val player = createMusicPlayer(scope)
            musicPlayer = player
            audioCache.attach(
                scope = scope,
                loader = { uri -> load(player, uri) },
                onDiscarded = { music -> player.dispose(music) },
            )
            stateManager.isFocused
                .debounce(musicPauseDelayOnFocusLoss.milliseconds)
                .filterNot { it }
                .onEach { audioCache.uris.forEach(::pause) }
                .launchIn(scope)
        }
    }

    private suspend fun load(player: MusicPlayer, uri: String): Any? {
        log(
            message = "Preloading ${uri}...",
            importance = Logger.Importance.LOW,
        )
        return player.preload(uri)?.also {
            log(
                message = "${uri.substringAfterLast('/')} preloaded.",
                importance = Logger.Importance.MEDIUM,
            )
        }
    }

    override fun getLoadingProgress(uri: String) = getLoadingProgress(setOf(uri))

    override fun getLoadingProgress(uris: Collection<String>) = if (uris.isEmpty()) flowOf(1f) else audioCache.entries.map { cache ->
        cache.filter { (key, _) -> key in uris }.count { (_, value) -> value != null }.toFloat() / uris.size
    }.distinctUntilChanged()

    override fun preload(vararg uris: String) = preload(uris.toSet())

    override fun preload(uris: Collection<String>) = uris.forEach(audioCache::preload)

    override fun isPlaying(uri: String) = audioCache.loaded(uri).let { music ->
        music != null && musicPlayer?.isPlaying(music) == true
    }

    override fun play(uri: String, shouldLoop: Boolean, shouldRestart: Boolean) {
        musicPlayer?.let { musicPlayer ->
            if (shouldRestart || !isPlaying(uri)) {
                scope.launch {
                    val music = audioCache.get(uri) ?: return@launch
                    if (stateManager.isFocused.value) {
                        // Apply volume configuration before playing
                        val volume = getVolume(uri)
                        musicPlayer.setVolume(music, volume.first, volume.second)
                        musicPlayer.play(music, shouldLoop, shouldRestart)
                    }
                }
            }
        }
    }

    override fun pause(uri: String) {
        if (isPlaying(uri)) {
            audioCache.loaded(uri)?.let { music -> musicPlayer?.pause(music) }
        }
    }

    override fun stop(uri: String) {
        if (isPlaying(uri)) {
            scope.launch {
                audioCache.loaded(uri)?.let { music -> musicPlayer?.stop(music) }
            }
        }
    }

    override fun unload(uri: String) {
        audioCache.remove(uri)?.let { music ->
            musicPlayer?.let { musicPlayer -> scope.launch { musicPlayer.dispose(music) } }
        }
    }

    override fun unloadAll() {
        val unloaded = audioCache.clear()
        val musicPlayer = musicPlayer ?: return
        scope.launch {
            unloaded.forEach { music -> musicPlayer.dispose(music) }
        }
    }

    override fun setVolume(uri: String, leftVolume: Float, rightVolume: Float) {
        // Store the volume configuration for this URI
        volumeConfig.update { it.putting(uri, Pair(leftVolume, rightVolume)) }

        // If the sound is currently playing, apply volume immediately
        audioCache.loaded(uri)?.let { music ->
            if (isPlaying(uri)) {
                musicPlayer?.setVolume(music, leftVolume, rightVolume)
            }
        }
    }

    override fun setDefaultVolume(leftVolume: Float, rightVolume: Float) {
        defaultVolume = Pair(leftVolume, rightVolume)
    }

    override fun getVolume(uri: String): Pair<Float, Float> {
        return volumeConfig.value[uri] ?: defaultVolume
    }

    override fun onDispose() {
        val unloaded = audioCache.clear()
        musicPlayer?.onManagerDisposed(unloaded)
        musicPlayer = null
    }
}
