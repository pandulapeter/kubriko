/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
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
import kubriko.examples.game_space_squadron.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
internal class AudioManager(
    private val stateManager: StateManager,
    private val userPreferencesManager: UserPreferencesManager
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
                    uri = Res.getUri(URI_MUSIC),
                    shouldLoop = true,
                )
            } else {
                musicManager.pause(Res.getUri(URI_MUSIC))
            }
        }.launchIn(scope)
        stateManager.isFocused
            .filter { it }
            .onEach { shouldStopMusic.update { false } }
            .launchIn(scope)
    }

    override fun onUpdate(deltaTimeInMilliseconds: Float, gameTimeMilliseconds: Long) {
        soundUrisToPlay.forEach { soundManager.play(Res.getUri(it)) }
        soundUrisToPlay.clear()
    }

    fun playButtonPlaySoundEffect() = playSoundEffect(URI_SOUND_BUTTON_PLAY)

    fun playButtonToggleSoundEffect() = playSoundEffect(URI_SOUND_BUTTON_TOGGLE)

    fun playButtonHoverSoundEffect() = playSoundEffect(URI_SOUND_BUTTON_HOVER)

    fun playShootSoundEffect() = playSoundEffect(URI_SOUND_SHOOT)

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

        fun getMusicUrisToPreload() = listOf(
            URI_MUSIC,
        ).map { Res.getUri(it) }

        fun getSoundUrisToPreload() = listOf(
            URI_SOUND_BUTTON_PLAY,
            URI_SOUND_BUTTON_TOGGLE,
            URI_SOUND_BUTTON_HOVER,
            URI_SOUND_SHOOT,
        ).map { Res.getUri(it) }
    }
}