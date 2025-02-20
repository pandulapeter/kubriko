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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedFont
import com.pandulapeter.kubriko.uiComponents.utilities.preloadedImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kubriko.examples.game_annoyed_penguins.generated.resources.Res
import kubriko.examples.game_annoyed_penguins.generated.resources.img_logo
import kubriko.examples.game_annoyed_penguins.generated.resources.permanent_marker
import org.jetbrains.compose.resources.DrawableResource

internal class LoadingManager : Manager() {
    private val musicManager by manager<MusicManager>()
    private val soundManager by manager<SoundManager>()
    private val spriteManager by manager<SpriteManager>()
    private val musicUris = AudioManager.getMusicUrisToPreload()
    private val soundUris = AudioManager.getSoundUrisToPreload()
    private val spriteResources = listOf<DrawableResource>()
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

    override fun onInitialize(kubriko: Kubriko) {
        musicManager.preload(musicUris)
        soundManager.preload(soundUris)
        spriteManager.preload(spriteResources)
    }

    @Composable
    fun isGameLoaded() = isInitialized.collectAsState().value
            && areMenuResourcesLoaded()
            && areGameResourcesLoaded.collectAsState().value

    @Composable
    override fun Composable(insetPaddingModifier: Modifier) {
        if (!isFontLoaded.value) {
            isFontLoaded.update { preloadedFont(Res.font.permanent_marker).value != null }
        }
    }

    @Composable
    private fun areMenuResourcesLoaded() = isFontLoaded.collectAsState().value
            && preloadedImageBitmap(Res.drawable.img_logo).value != null
}