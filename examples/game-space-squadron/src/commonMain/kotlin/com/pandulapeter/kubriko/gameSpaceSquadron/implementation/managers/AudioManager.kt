/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.utilities.getResourceUri
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

    fun playButtonPlaySoundEffect() = playSoundEffect(URI_SOUND_BUTTON_PLAY)

    fun playButtonToggleSoundEffect() = playSoundEffect(URI_SOUND_BUTTON_TOGGLE)

    fun playButtonHoverSoundEffect() = playSoundEffect(URI_SOUND_BUTTON_HOVER)

    fun playShootSoundEffect() = playSoundEffect(URI_SOUND_SHOOT)

    fun playShootAlienSoundEffect() = playSoundEffect(URI_SOUND_SHOOT_ALIEN)

    fun playShipHitSoundEffect() = playSoundEffect(URI_SOUND_SHIP_HIT)

    fun playExplosionSmallSoundEffect() = playSoundEffect(URI_SOUND_EXPLOSION_SMALL)

    fun playExplosionLargeSoundEffect() = playSoundEffect(URI_SOUND_EXPLOSION_LARGE)

    fun playPowerUpSoundEffect() = playSoundEffect(URI_SOUND_POWER_UP)

    fun playShieldSoundEffect() = playSoundEffect(URI_SOUND_SHIELD)

    fun stopMusicBeforeDispose() = shouldStopMusic.update { true }

    private fun playSoundEffect(uri: String) {
        if (userPreferencesManager.areSoundEffectsEnabled.value) {
            soundUrisToPlay.add(uri)
        }
    }

    companion object {
        private const val URI_MUSIC = "files/music/music.mp3"
        private const val URI_SOUND_BUTTON_PLAY = "files/sounds/button_play.wav"
        private const val URI_SOUND_BUTTON_TOGGLE = "files/sounds/button_toggle.wav"
        private const val URI_SOUND_BUTTON_HOVER = "files/sounds/button_hover.wav"
        private const val URI_SOUND_SHOOT = "files/sounds/shoot.wav"
        private const val URI_SOUND_SHOOT_ALIEN = "files/sounds/shoot_alien.wav"
        private const val URI_SOUND_SHIP_HIT = "files/sounds/ship_hit.wav"
        private const val URI_SOUND_EXPLOSION_SMALL = "files/sounds/explosion_small.wav"
        private const val URI_SOUND_EXPLOSION_LARGE = "files/sounds/explosion_large.wav"
        private const val URI_SOUND_POWER_UP = "files/sounds/power_up.wav"
        private const val URI_SOUND_SHIELD = "files/sounds/shield.wav"

        fun getMusicUrisToPreload(webRootPathName: String) = listOf(
            URI_MUSIC,
        ).map { getResourceUri(it, webRootPathName) }

        fun getSoundUrisToPreload(webRootPathName: String) = listOf(
            URI_SOUND_BUTTON_PLAY,
            URI_SOUND_BUTTON_TOGGLE,
            URI_SOUND_BUTTON_HOVER,
            URI_SOUND_SHOOT,
            URI_SOUND_SHOOT_ALIEN,
            URI_SOUND_SHIP_HIT,
            URI_SOUND_EXPLOSION_SMALL,
            URI_SOUND_EXPLOSION_LARGE,
            URI_SOUND_POWER_UP,
            URI_SOUND_SHIELD,
        ).map { getResourceUri(it, webRootPathName) }
    }
}