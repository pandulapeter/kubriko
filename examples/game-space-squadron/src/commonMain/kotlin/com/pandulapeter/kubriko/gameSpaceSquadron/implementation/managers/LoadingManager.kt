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
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kubriko.examples.game_space_squadron.generated.resources.Res
import kubriko.examples.game_space_squadron.generated.resources.back
import kubriko.examples.game_space_squadron.generated.resources.close_confirmation
import kubriko.examples.game_space_squadron.generated.resources.close_confirmation_negative
import kubriko.examples.game_space_squadron.generated.resources.close_confirmation_positive
import kubriko.examples.game_space_squadron.generated.resources.fullscreen_enter
import kubriko.examples.game_space_squadron.generated.resources.fullscreen_exit
import kubriko.examples.game_space_squadron.generated.resources.ic_back
import kubriko.examples.game_space_squadron.generated.resources.ic_exit
import kubriko.examples.game_space_squadron.generated.resources.ic_fullscreen_enter
import kubriko.examples.game_space_squadron.generated.resources.ic_fullscreen_exit
import kubriko.examples.game_space_squadron.generated.resources.ic_information
import kubriko.examples.game_space_squadron.generated.resources.ic_music_off
import kubriko.examples.game_space_squadron.generated.resources.ic_music_on
import kubriko.examples.game_space_squadron.generated.resources.ic_pause
import kubriko.examples.game_space_squadron.generated.resources.ic_play
import kubriko.examples.game_space_squadron.generated.resources.ic_sound_effects_off
import kubriko.examples.game_space_squadron.generated.resources.ic_sound_effects_on
import kubriko.examples.game_space_squadron.generated.resources.img_logo
import kubriko.examples.game_space_squadron.generated.resources.information
import kubriko.examples.game_space_squadron.generated.resources.information_contents
import kubriko.examples.game_space_squadron.generated.resources.music_disable
import kubriko.examples.game_space_squadron.generated.resources.music_enable
import kubriko.examples.game_space_squadron.generated.resources.orbitron
import kubriko.examples.game_space_squadron.generated.resources.pause
import kubriko.examples.game_space_squadron.generated.resources.play
import kubriko.examples.game_space_squadron.generated.resources.score
import kubriko.examples.game_space_squadron.generated.resources.sound_effects_disable
import kubriko.examples.game_space_squadron.generated.resources.sound_effects_enable
import kubriko.examples.game_space_squadron.generated.resources.sprite_alien_ship
import kubriko.examples.game_space_squadron.generated.resources.sprite_power_up
import kubriko.examples.game_space_squadron.generated.resources.sprite_shield
import kubriko.examples.game_space_squadron.generated.resources.sprite_ship

internal class LoadingManager : Manager() {
    private val musicManager by manager<MusicManager>()
    private val soundManager by manager<SoundManager>()
    private val spriteManager by manager<SpriteManager>()
    private val musicUris = AudioManager.getMusicUrisToPreload()
    private val soundUris = AudioManager.getSoundUrisToPreload()
    private val spriteResources = listOf(
        Res.drawable.sprite_ship,
        Res.drawable.sprite_alien_ship,
        Res.drawable.sprite_power_up,
        Res.drawable.sprite_shield,
    )
    private val areGameResourcesLoaded by autoInitializingLazy {
        combine(
            musicManager.getLoadingProgress(musicUris),
            soundManager.getLoadingProgress(soundUris),
            spriteManager.getLoadingProgress(spriteResources),
        ) { musicLoadingProgress, soundLoadingProgress, spriteLoadingProgress ->
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
            isFontLoaded.update { preloadedFont(Res.font.orbitron).value != null }
        }
    }

    @Composable
    private fun areMenuResourcesLoaded() = isFontLoaded.collectAsState().value
            && areIconResourcesLoaded()
            && areImageResourcesLoaded()
            && areStringResourcesLoaded()

    @Composable
    private fun areIconResourcesLoaded() = preloadedImageVector(Res.drawable.ic_exit).value != null
            && preloadedImageVector(Res.drawable.ic_fullscreen_enter).value != null
            && preloadedImageVector(Res.drawable.ic_fullscreen_exit).value != null
            && preloadedImageVector(Res.drawable.ic_information).value != null
            && preloadedImageVector(Res.drawable.ic_music_off).value != null
            && preloadedImageVector(Res.drawable.ic_music_on).value != null
            && preloadedImageVector(Res.drawable.ic_pause).value != null
            && preloadedImageVector(Res.drawable.ic_play).value != null
            && preloadedImageVector(Res.drawable.ic_sound_effects_off).value != null
            && preloadedImageVector(Res.drawable.ic_sound_effects_on).value != null
            && preloadedImageVector(Res.drawable.ic_back).value != null

    @Composable
    private fun areImageResourcesLoaded() = preloadedImageBitmap(Res.drawable.img_logo).value != null

    @Composable
    private fun areStringResourcesLoaded() = preloadedString(Res.string.play).value.isNotBlank()
            && preloadedString(Res.string.pause).value.isNotBlank()
            && preloadedString(Res.string.back).value.isNotBlank()
            && preloadedString(Res.string.information).value.isNotBlank()
            && preloadedString(Res.string.sound_effects_enable).value.isNotBlank()
            && preloadedString(Res.string.sound_effects_disable).value.isNotBlank()
            && preloadedString(Res.string.music_enable).value.isNotBlank()
            && preloadedString(Res.string.music_disable).value.isNotBlank()
            && preloadedString(Res.string.fullscreen_enter).value.isNotBlank()
            && preloadedString(Res.string.fullscreen_exit).value.isNotBlank()
            && preloadedString(Res.string.information_contents).value.isNotBlank()
            && preloadedString(Res.string.score).value.isNotBlank()
            && preloadedString(Res.string.close_confirmation).value.isNotBlank()
            && preloadedString(Res.string.close_confirmation_positive).value.isNotBlank()
            && preloadedString(Res.string.close_confirmation_negative).value.isNotBlank()
}