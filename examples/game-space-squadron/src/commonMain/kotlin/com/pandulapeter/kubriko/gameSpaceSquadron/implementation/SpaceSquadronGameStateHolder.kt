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
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.AudioManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.BackgroundAnimationManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.GameplayManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.LoadingManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.ScoreManager
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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
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
    private val backgroundStateManager = StateManager.newInstance()
    private val backgroundShaderManager = ShaderManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG_BACKGROUND,
    )
    private val backgroundAnimationManager = BackgroundAnimationManager()
    val backgroundKubriko = Kubriko.newInstance(
        backgroundStateManager,
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
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    val stateManager = StateManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val persistenceManager = PersistenceManager.newInstance(
        fileName = "kubrikoSpaceSquadron",
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val scoreManager = ScoreManager(persistenceManager)
    val userPreferencesManager = UserPreferencesManager(persistenceManager)
    val audioManager = AudioManager(stateManager, userPreferencesManager)
    private val particleManager = ParticleManager.newInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    val gameplayManager = GameplayManager(backgroundStateManager)
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
            scoreManager,
            uiManager,
            audioManager,
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    )
    override val kubriko = _kubriko.asStateFlow()

    override fun stopMusic() = audioManager.stopMusicBeforeDispose()

    override val backNavigationIntent = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun navigateBack(
        isInFullscreenMode: Boolean,
        onFullscreenModeToggled: () -> Unit,
    ) = true.also {
        if (stateManager.isRunning.value && !gameplayManager.isGameOver.value) {
            gameplayManager.pauseGame()
        } else if (uiManager.isInfoDialogVisible.value) {
            uiManager.toggleInfoDialogVisibility()
        } else if (gameplayManager.isGameStarted) {
            gameplayManager.playGame()
        } else if (isInFullscreenMode) {
            audioManager.playButtonToggleSoundEffect()
            onFullscreenModeToggled()
        } else {
            // TODO: Dismiss close confirmation dialog
        }
    }

    override fun dispose() {
        backgroundKubriko.dispose()
        kubriko.value.dispose()
    }
}

private const val LOG_TAG = "SS"
private const val LOG_TAG_BACKGROUND = "$LOG_TAG-Background"