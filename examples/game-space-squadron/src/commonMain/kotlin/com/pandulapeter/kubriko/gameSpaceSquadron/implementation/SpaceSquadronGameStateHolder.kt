/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.collision.CollisionManager
import com.pandulapeter.kubriko.gameSpaceSquadron.ViewportHeight
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.AudioManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.BackgroundAnimationManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.GameplayManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.LoadingManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.UIManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.UserPreferencesManager
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.particles.ParticleManager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.shaders.ShaderManager
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.sprites.SpriteManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SpaceSquadronGameStateHolder : StateHolder

internal class SpaceSquadronGameStateHolderImpl : SpaceSquadronGameStateHolder {

    private val sharedMusicManager = MusicManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val sharedSoundManager = SoundManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val sharedSpriteManager = SpriteManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    val backgroundLoadingManager = LoadingManager()
    private val backgroundShaderManager = ShaderManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG_BACKGROUND,
    )
    private val backgroundAnimationManager = BackgroundAnimationManager()
    val backgroundKubriko = Kubriko.newInstance(
        sharedMusicManager,
        sharedSoundManager,
        sharedSpriteManager,
        backgroundShaderManager,
        backgroundLoadingManager,
        backgroundAnimationManager,
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG_BACKGROUND,
    )
    private val viewportManager = ViewportManager.newInstance(
        aspectRatioMode = ViewportManager.AspectRatioMode.FitVertical(
            height = ViewportHeight,
        ),
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    val stateManager = StateManager.newInstance(
        shouldAutoStart = false,
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val persistenceManager = PersistenceManager.newInstance(
        fileName = "kubrikoSpaceSquadron",
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    val userPreferencesManager = UserPreferencesManager(persistenceManager)
    val audioManager = AudioManager(stateManager, userPreferencesManager)
    private val particleManager = ParticleManager.newInstance()
    val gameplayManager = GameplayManager()
    val uiManager = UIManager(stateManager)
    private val collisionManager = CollisionManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val keyboardInputManager = KeyboardInputManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val pointerInputManager = PointerInputManager.newInstance(
        isActiveAboveViewport = true,
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val _kubriko = MutableStateFlow(
        Kubriko.newInstance(
            sharedMusicManager,
            sharedSoundManager,
            sharedSpriteManager,
            stateManager,
            viewportManager,
            collisionManager,
            keyboardInputManager,
            pointerInputManager,
            persistenceManager,
            userPreferencesManager,
            particleManager,
            gameplayManager,
            uiManager,
            audioManager,
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    )
    override val kubriko = _kubriko.asStateFlow()

    override fun stopMusic() = audioManager.stopMusicBeforeDispose()

    override fun dispose() {
        kubriko.value.dispose()
        backgroundKubriko.dispose()
    }
}

private const val LOG_TAG = "SS"
private const val LOG_TAG_BACKGROUND = "$LOG_TAG-Background"