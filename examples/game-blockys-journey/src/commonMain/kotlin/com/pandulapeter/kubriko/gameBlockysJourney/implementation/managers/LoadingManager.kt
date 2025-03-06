/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameBlockysJourney.implementation.managers

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedFont
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageBitmap
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageVector
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kubriko.examples.game_blockys_journey.generated.resources.Res
import kubriko.examples.game_blockys_journey.generated.resources.back
import kubriko.examples.game_blockys_journey.generated.resources.close_confirmation
import kubriko.examples.game_blockys_journey.generated.resources.close_confirmation_negative
import kubriko.examples.game_blockys_journey.generated.resources.close_confirmation_positive
import kubriko.examples.game_blockys_journey.generated.resources.fullscreen_enter
import kubriko.examples.game_blockys_journey.generated.resources.fullscreen_exit
import kubriko.examples.game_blockys_journey.generated.resources.ic_back
import kubriko.examples.game_blockys_journey.generated.resources.ic_exit
import kubriko.examples.game_blockys_journey.generated.resources.ic_fullscreen_enter
import kubriko.examples.game_blockys_journey.generated.resources.ic_fullscreen_exit
import kubriko.examples.game_blockys_journey.generated.resources.ic_information
import kubriko.examples.game_blockys_journey.generated.resources.ic_music_off
import kubriko.examples.game_blockys_journey.generated.resources.ic_music_on
import kubriko.examples.game_blockys_journey.generated.resources.ic_pause
import kubriko.examples.game_blockys_journey.generated.resources.ic_sound_effects_off
import kubriko.examples.game_blockys_journey.generated.resources.ic_sound_effects_on
import kubriko.examples.game_blockys_journey.generated.resources.img_logo
import kubriko.examples.game_blockys_journey.generated.resources.information
import kubriko.examples.game_blockys_journey.generated.resources.information_contents
import kubriko.examples.game_blockys_journey.generated.resources.medieval_sharp
import kubriko.examples.game_blockys_journey.generated.resources.music_disable
import kubriko.examples.game_blockys_journey.generated.resources.music_enable
import kubriko.examples.game_blockys_journey.generated.resources.pause
import kubriko.examples.game_blockys_journey.generated.resources.play
import kubriko.examples.game_blockys_journey.generated.resources.sound_effects_disable
import kubriko.examples.game_blockys_journey.generated.resources.sound_effects_enable
import kubriko.examples.game_blockys_journey.generated.resources.sprite_blocky_east
import kubriko.examples.game_blockys_journey.generated.resources.sprite_blocky_north
import kubriko.examples.game_blockys_journey.generated.resources.sprite_blocky_north_east
import kubriko.examples.game_blockys_journey.generated.resources.sprite_blocky_north_west
import kubriko.examples.game_blockys_journey.generated.resources.sprite_blocky_south
import kubriko.examples.game_blockys_journey.generated.resources.sprite_blocky_south_east
import kubriko.examples.game_blockys_journey.generated.resources.sprite_blocky_south_west
import kubriko.examples.game_blockys_journey.generated.resources.sprite_blocky_west
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException

internal class LoadingManager(
    webRootPathName: String,
) : Manager() {
    private val musicManager by manager<MusicManager>()
    private val soundManager by manager<SoundManager>()
    private val spriteManager by manager<SpriteManager>()
    private val musicUris = AudioManager.getMusicUrisToPreload(webRootPathName)
    private val soundUris = AudioManager.getSoundUrisToPreload(webRootPathName)
    private val serializationManager by manager<SerializationManager<EditableMetadata<*>, Editable<*>>>()
    private val spriteResources = listOf(
        Res.drawable.sprite_blocky_east,
        Res.drawable.sprite_blocky_north,
        Res.drawable.sprite_blocky_north_east,
        Res.drawable.sprite_blocky_north_west,
        Res.drawable.sprite_blocky_south,
        Res.drawable.sprite_blocky_south_east,
        Res.drawable.sprite_blocky_south_west,
        Res.drawable.sprite_blocky_west,
    )
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
    private val isLevelLoaded = MutableStateFlow(false)
    var isLoadingDone = false
        private set
    var actors = emptyList<Actor>()
        private set

    @OptIn(ExperimentalResourceApi::class)
    override fun onInitialize(kubriko: Kubriko) {
        musicManager.preload(musicUris)
        soundManager.preload(soundUris)
        spriteManager.preload(spriteResources)
        scope.launch {
            try {
                actors = serializationManager.deserializeActors(Res.readBytes("files/scenes/world.json").decodeToString())
            } catch (_: MissingResourceException) {
            }
            isLevelLoaded.update { true }
        }
    }

    @Composable
    fun isGameLoaded() = (isInitialized.collectAsState().value
            && areMenuResourcesLoaded()
            && isLevelLoaded.collectAsState().value
            && areGameResourcesLoaded.collectAsState().value).also {
        isLoadingDone = it
    }

    @Composable
    override fun Composable(windowInsets: WindowInsets) {
        if (!isFontLoaded.value) {
            isFontLoaded.update { preloadedFont(Res.font.medieval_sharp).value != null }
        }
    }

    @Composable
    private fun areMenuResourcesLoaded() = isFontLoaded.collectAsState().value
            && areIconResourcesLoaded()
            && areImageResourcesLoaded()
            && areStringResourcesLoaded()

    @Composable
    private fun areIconResourcesLoaded() = preloadedImageVector(Res.drawable.ic_back).value != null
            && preloadedImageVector(Res.drawable.ic_exit).value != null
            && preloadedImageVector(Res.drawable.ic_fullscreen_enter).value != null
            && preloadedImageVector(Res.drawable.ic_fullscreen_exit).value != null
            && preloadedImageVector(Res.drawable.ic_information).value != null
            && preloadedImageVector(Res.drawable.ic_music_off).value != null
            && preloadedImageVector(Res.drawable.ic_music_on).value != null
            && preloadedImageVector(Res.drawable.ic_pause).value != null
            && preloadedImageVector(Res.drawable.ic_sound_effects_off).value != null
            && preloadedImageVector(Res.drawable.ic_sound_effects_on).value != null

    @Composable
    private fun areImageResourcesLoaded() = preloadedImageBitmap(Res.drawable.img_logo).value != null

    @Composable
    private fun areStringResourcesLoaded() = preloadedString(Res.string.back).value.isNotBlank()
            && preloadedString(Res.string.pause).value.isNotBlank()
            && preloadedString(Res.string.information).value.isNotBlank()
            && preloadedString(Res.string.sound_effects_enable).value.isNotBlank()
            && preloadedString(Res.string.sound_effects_disable).value.isNotBlank()
            && preloadedString(Res.string.music_enable).value.isNotBlank()
            && preloadedString(Res.string.music_disable).value.isNotBlank()
            && preloadedString(Res.string.fullscreen_enter).value.isNotBlank()
            && preloadedString(Res.string.fullscreen_exit).value.isNotBlank()
            && preloadedString(Res.string.information_contents).value.isNotBlank()
            && preloadedString(Res.string.play).value.isNotBlank()
            && preloadedString(Res.string.close_confirmation).value.isNotBlank()
            && preloadedString(Res.string.close_confirmation_positive).value.isNotBlank()
            && preloadedString(Res.string.close_confirmation_negative).value.isNotBlank()
}