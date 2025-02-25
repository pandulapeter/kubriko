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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedFont
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageBitmap
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageVector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kubriko.examples.game_annoyed_penguins.generated.resources.Res
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_back
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_exit
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_fullscreen_enter
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_fullscreen_exit
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_information
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_music_off
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_music_on
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_pause
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_sound_effects_off
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_sound_effects_on
import kubriko.examples.game_annoyed_penguins.generated.resources.img_logo
import kubriko.examples.game_annoyed_penguins.generated.resources.permanent_marker
import kubriko.examples.game_annoyed_penguins.generated.resources.sprite_penguin

internal class LoadingManager : Manager() {
    private val musicManager by manager<MusicManager>()
    private val soundManager by manager<SoundManager>()
    private val spriteManager by manager<SpriteManager>()
    private val musicUris = AudioManager.getMusicUrisToPreload()
    private val soundUris = AudioManager.getSoundUrisToPreload()
    private val spriteResources = listOf(Res.drawable.sprite_penguin)
    private val areGameResourcesLoaded by autoInitializingLazy {
        combine(
            soundManager.getLoadingProgress(soundUris),
            spriteManager.getLoadingProgress(spriteResources),
            musicManager.getLoadingProgress(musicUris),
        ) { soundLoadingProgress, spriteLoadingProgress, musicLoadingProgress ->
            musicLoadingProgress == 1f && soundLoadingProgress == 1f && spriteLoadingProgress == 1f
        }.asStateFlow(false)
    }
    private val isFontLoaded = MutableStateFlow(false)
    var isLoadingDone = false
        private set

    override fun onInitialize(kubriko: Kubriko) {
        musicManager.preload(musicUris)
        soundManager.preload(soundUris)
        spriteManager.preload(spriteResources)
    }

    @Composable
    fun isGameLoaded() = (isInitialized.collectAsState().value
            && areMenuResourcesLoaded()
            && areGameResourcesLoaded.collectAsState().value).also {
        isLoadingDone = it
    }

    @Composable
    override fun Composable(windowInsets: WindowInsets) {
        if (!isFontLoaded.value) {
            isFontLoaded.update { preloadedFont(Res.font.permanent_marker).value != null }
        }
    }

    @Composable
    private fun areMenuResourcesLoaded() = isFontLoaded.collectAsState().value
            && preloadedImageVector(Res.drawable.ic_back).value != null
            && preloadedImageVector(Res.drawable.ic_exit).value != null
            && preloadedImageVector(Res.drawable.ic_fullscreen_enter).value != null
            && preloadedImageVector(Res.drawable.ic_fullscreen_exit).value != null
            && preloadedImageVector(Res.drawable.ic_information).value != null
            && preloadedImageVector(Res.drawable.ic_music_off).value != null
            && preloadedImageVector(Res.drawable.ic_music_on).value != null
            && preloadedImageVector(Res.drawable.ic_pause).value != null
            && preloadedImageVector(Res.drawable.ic_sound_effects_off).value != null
            && preloadedImageVector(Res.drawable.ic_sound_effects_on).value != null
            && preloadedImageBitmap(Res.drawable.img_logo).value != null
}