/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameWallbreaker.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.utilities.getResourceUri
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class AudioManager(
    private val stateManager: StateManager,
    private val userPreferencesManager: UserPreferencesManager,
    private val webRootPathName: String,
) : Manager() {
    private val musicManager by manager<MusicManager>()
    private val soundManager by manager<SoundManager>()
    private val soundUrisToPlay = mutableSetOf<String>()
    private val shouldStopMusic = MutableStateFlow(false)

    @OptIn(FlowPreview::class)
    override fun onInitialize(kubriko: Kubriko) {
        combine(
            stateManager.isFocused.debounce(100),
            userPreferencesManager.isMusicEnabled,
            shouldStopMusic,
        ) { isFocused, isMusicEnabled, shouldStopMusic ->
            Triple(isFocused, isMusicEnabled, shouldStopMusic)
        }.distinctUntilChanged().onEach { (isFocused, isMusicEnabled, shouldStopMusic) ->
            if (isMusicEnabled && isFocused && !shouldStopMusic) {
                musicManager.play(
                    uri = getResourceUri(URI_MUSIC, webRootPathName),
                    shouldLoop = true,
                )
            } else {
                musicManager.pause(getResourceUri(URI_MUSIC, webRootPathName))
            }
        }.launchIn(scope)
        stateManager.isFocused
            .filter { it }
            .onEach { shouldStopMusic.update { false } }
            .launchIn(scope)
    }

    override fun onUpdate(deltaTimeInMilliseconds: Int) {
        soundUrisToPlay.forEach { soundManager.play(getResourceUri(it, webRootPathName)) }
        soundUrisToPlay.clear()
    }

    fun playBrickPopSoundEffect() = playSoundEffect(URI_SOUND_BRICK_POP)

    fun playEdgeBounceSoundEffect() = playSoundEffect(URI_SOUND_EDGE_BOUNCE)

    fun playGameOverSoundEffect() = playSoundEffect(URI_SOUND_GAME_OVER)

    fun playLevelClearedSoundEffect() = playSoundEffect(URI_SOUND_LEVEL_CLEARED)

    fun playPaddleHitSoundEffect() = playSoundEffect(URI_SOUND_PADDLE_HIT)

    fun playClickSoundEffect() = playSoundEffect(URI_SOUND_CLICK)

    fun playHoverSoundEffect() = playSoundEffect(URI_SOUND_HOVER)

    fun stopMusicBeforeDispose() = shouldStopMusic.update { true }

    private fun playSoundEffect(uri: String) {
        if (userPreferencesManager.areSoundEffectsEnabled.value) {
            soundUrisToPlay.add(uri)
        }
    }

    companion object {
        private const val URI_MUSIC = "files/music/music.mp3"
        private const val URI_SOUND_BRICK_POP = "files/sounds/brick_pop.wav"
        private const val URI_SOUND_EDGE_BOUNCE = "files/sounds/edge_bounce.wav"
        private const val URI_SOUND_GAME_OVER = "files/sounds/game_over.wav"
        private const val URI_SOUND_LEVEL_CLEARED = "files/sounds/level_cleared.wav"
        private const val URI_SOUND_PADDLE_HIT = "files/sounds/paddle_hit.wav"
        private const val URI_SOUND_CLICK = "files/sounds/button_click.wav"
        private const val URI_SOUND_HOVER = "files/sounds/button_hover.wav"

        fun getMusicUrisToPreload(webRootPathName: String) = listOf(
            URI_MUSIC,
        ).map { getResourceUri(it, webRootPathName) }

        fun getSoundUrisToPreload(webRootPathName: String) = listOf(
            URI_SOUND_BRICK_POP,
            URI_SOUND_EDGE_BOUNCE,
            URI_SOUND_GAME_OVER,
            URI_SOUND_LEVEL_CLEARED,
            URI_SOUND_PADDLE_HIT,
            URI_SOUND_CLICK,
            URI_SOUND_HOVER,
        ).map { getResourceUri(it, webRootPathName) }
    }
}