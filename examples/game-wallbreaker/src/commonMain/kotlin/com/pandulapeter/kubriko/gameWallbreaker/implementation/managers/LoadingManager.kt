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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedFont
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageBitmap
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageVector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kubriko.examples.game_wallbreaker.generated.resources.Res
import kubriko.examples.game_wallbreaker.generated.resources.ic_close
import kubriko.examples.game_wallbreaker.generated.resources.ic_exit
import kubriko.examples.game_wallbreaker.generated.resources.ic_fullscreen_enter
import kubriko.examples.game_wallbreaker.generated.resources.ic_fullscreen_exit
import kubriko.examples.game_wallbreaker.generated.resources.ic_information
import kubriko.examples.game_wallbreaker.generated.resources.ic_music_off
import kubriko.examples.game_wallbreaker.generated.resources.ic_music_on
import kubriko.examples.game_wallbreaker.generated.resources.ic_pause
import kubriko.examples.game_wallbreaker.generated.resources.ic_play
import kubriko.examples.game_wallbreaker.generated.resources.ic_sound_effects_off
import kubriko.examples.game_wallbreaker.generated.resources.ic_sound_effects_on
import kubriko.examples.game_wallbreaker.generated.resources.img_logo
import kubriko.examples.game_wallbreaker.generated.resources.kanit_regular

internal class LoadingManager : Manager() {

    private val musicManager by manager<MusicManager>()
    private val soundManager by manager<SoundManager>()
    private val musicUris = AudioManager.getMusicUrisToPreload()
    private val soundUris = AudioManager.getSoundUrisToPreload()
    private val areGameResourcesLoaded by autoInitializingLazy {
        combine(
            musicManager.getLoadingProgress(musicUris),
            soundManager.getLoadingProgress(soundUris),
        ) { musicLoadingProgress, soundLoadingProgress ->
            musicLoadingProgress == 1f && soundLoadingProgress == 1f
        }.asStateFlow(false)
    }
    private val isFontLoaded = MutableStateFlow(false)

    override fun onInitialize(kubriko: Kubriko) {
        musicManager.preload(musicUris)
        soundManager.preload(soundUris)
    }

    @Composable
    fun isGameLoaded() = isInitialized.collectAsState().value
            && areMenuResourcesLoaded()
            && areGameResourcesLoaded.collectAsState().value

    @Composable
    override fun Composable(windowInsets: WindowInsets) {
        if (!isFontLoaded.value) {
            isFontLoaded.update { preloadedFont(Res.font.kanit_regular).value != null }
        }
    }

    @Composable
    private fun areMenuResourcesLoaded() = isFontLoaded.collectAsState().value
            && preloadedImageVector(Res.drawable.ic_exit).value != null
            && preloadedImageVector(Res.drawable.ic_fullscreen_enter).value != null
            && preloadedImageVector(Res.drawable.ic_fullscreen_exit).value != null
            && preloadedImageVector(Res.drawable.ic_information).value != null
            && preloadedImageVector(Res.drawable.ic_music_off).value != null
            && preloadedImageVector(Res.drawable.ic_music_on).value != null
            && preloadedImageVector(Res.drawable.ic_pause).value != null
            && preloadedImageVector(Res.drawable.ic_play).value != null
            && preloadedImageVector(Res.drawable.ic_sound_effects_off).value != null
            && preloadedImageVector(Res.drawable.ic_sound_effects_on).value != null
            && preloadedImageVector(Res.drawable.ic_close).value != null
            && preloadedImageBitmap(Res.drawable.img_logo).value != null
}