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
import com.pandulapeter.kubriko.audioPlayback.implementation.SoundPlayer
import com.pandulapeter.kubriko.audioPlayback.implementation.createSoundPlayer
import com.pandulapeter.kubriko.logger.Logger
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class SoundManagerImpl(
    private val maximumSimultaneousStreamsOfTheSameSound: Int,
    isLoggingEnabled: Boolean,
    instanceNameForLogging: String?,
) : SoundManager(isLoggingEnabled, instanceNameForLogging) {
    private val audioCache = AudioCache()
    private var soundPlayer: SoundPlayer? = null
    private val stateManager by manager<StateManager>()

    @Composable
    override fun Composable(windowInsets: WindowInsets) {
        if (soundPlayer == null) {
            val player = createSoundPlayer(maximumSimultaneousStreamsOfTheSameSound)
            soundPlayer = player
            audioCache.attach(
                scope = scope,
                loader = { uri -> load(player, uri) },
                onDiscarded = { sound -> player.dispose(sound) },
            )
        }
    }

    private suspend fun load(player: SoundPlayer, uri: String): Any? {
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

    override fun getLoadingProgress(uris: Collection<String>) = if (uris.isEmpty()) flowOf(1f) else audioCache.entries.map { cache ->
        cache.filter { (key, _) -> key in uris }.count { (_, value) -> value != null }.toFloat() / uris.size
    }.distinctUntilChanged()

    override fun preload(vararg uris: String) = preload(uris.toSet())

    override fun preload(uris: Collection<String>) = uris.forEach(audioCache::preload)

    override fun play(uri: String) {
        scope.launch {
            if (stateManager.isFocused.value) {
                soundPlayer?.let { soundPlayer ->
                    audioCache.get(uri)?.let { sound -> soundPlayer.play(sound) }
                }
            }
        }
    }

    override fun unload(uri: String) {
        audioCache.remove(uri)?.let { sound ->
            soundPlayer?.let { soundPlayer -> scope.launch { soundPlayer.dispose(sound) } }
        }
    }

    override fun onDispose() {
        val unloaded = audioCache.clear()
        soundPlayer?.let { soundPlayer ->
            unloaded.forEach { sound -> soundPlayer.dispose(sound) }
            soundPlayer.dispose()
        }
        soundPlayer = null
    }
}
