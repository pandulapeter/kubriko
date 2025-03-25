/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.utilities.getResourceUri
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
    private val shouldPlayStretchingSound = MutableStateFlow(false)
    private val crashSoundEffectUris = listOf(
        URI_SOUND_CRASH_01,
        URI_SOUND_CRASH_02,
        URI_SOUND_CRASH_03,
        URI_SOUND_CRASH_04,
        URI_SOUND_CRASH_05,
        URI_SOUND_CRASH_06,
    )
    private var timeSinceLastPop = 0

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
        combine(
            stateManager.isRunning.debounce(100),
            userPreferencesManager.areSoundEffectsEnabled,
            shouldStopMusic,
            shouldPlayStretchingSound,
        ) { isRunning, areSoundEffectsEnabled, shouldStopMusic, shouldPlayStretchingSound ->
            Triple(isRunning, areSoundEffectsEnabled, shouldPlayStretchingSound && !shouldStopMusic)
        }.distinctUntilChanged().onEach { (isRunning, areSoundEffectsEnabled, shouldPlayStretchingSound) ->
            if (areSoundEffectsEnabled && isRunning && shouldPlayStretchingSound) {
                musicManager.play(
                    uri = getResourceUri(URI_SOUND_STRETCHING, webRootPathName),
                    shouldLoop = true,
                )
            } else {
                musicManager.pause(getResourceUri(URI_SOUND_STRETCHING, webRootPathName))
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
        if (timeSinceLastPop < POP_DEBOUNCE) {
            timeSinceLastPop += deltaTimeInMilliseconds
        }
    }

    fun playButtonToggleSoundEffect() = playSoundEffect(URI_SOUND_BUTTON_TOGGLE)

    fun playButtonHoverSoundEffect() = playSoundEffect(URI_SOUND_BUTTON_HOVER)

    fun playLaunchSoundEffect() = playSoundEffect(URI_SOUND_LAUNCH)

    fun playCrashSoundEffect() = playSoundEffect(crashSoundEffectUris.random())

    fun playPopSoundEffect() {
        if (timeSinceLastPop >= POP_DEBOUNCE) {
            timeSinceLastPop = 0
            playSoundEffect(URI_POP)
        }
    }

    fun setShouldPlayStretchingSoundEffect(shouldPlay: Boolean) = shouldPlayStretchingSound.update { shouldPlay }

    fun stopMusicBeforeDispose() = shouldStopMusic.update { true }

    private fun playSoundEffect(uri: String) {
        if (userPreferencesManager.areSoundEffectsEnabled.value) {
            soundUrisToPlay.add(uri)
        }
    }

    companion object {
        private const val POP_DEBOUNCE = 100
        private const val URI_MUSIC = "files/music/music.mp3"
        private const val URI_SOUND_STRETCHING = "files/sounds/stretching.mp3"
        private const val URI_SOUND_BUTTON_TOGGLE = "files/sounds/button_toggle.wav"
        private const val URI_SOUND_BUTTON_HOVER = "files/sounds/button_hover.wav"
        private const val URI_SOUND_LAUNCH = "files/sounds/launch.wav"
        private const val URI_SOUND_CRASH_01 = "files/sounds/crash_01.wav"
        private const val URI_SOUND_CRASH_02 = "files/sounds/crash_02.wav"
        private const val URI_SOUND_CRASH_03 = "files/sounds/crash_03.wav"
        private const val URI_SOUND_CRASH_04 = "files/sounds/crash_04.wav"
        private const val URI_SOUND_CRASH_05 = "files/sounds/crash_05.wav"
        private const val URI_SOUND_CRASH_06 = "files/sounds/crash_06.wav"
        private const val URI_POP = "files/sounds/pop.wav"

        fun getMusicUrisToPreload(webRootPathName: String) = listOf(
            URI_MUSIC,
            URI_SOUND_STRETCHING,
        ).map { getResourceUri(it, webRootPathName) }

        fun getSoundUrisToPreload(webRootPathName: String) = listOf(
            URI_SOUND_BUTTON_TOGGLE,
            URI_SOUND_BUTTON_HOVER,
            URI_SOUND_LAUNCH,
            URI_SOUND_CRASH_01,
            URI_SOUND_CRASH_02,
            URI_SOUND_CRASH_03,
            URI_SOUND_CRASH_04,
            URI_SOUND_CRASH_05,
            URI_SOUND_CRASH_06,
            URI_POP,
        ).map { getResourceUri(it, webRootPathName) }
    }
}