/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
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
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.GalaxyShader
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.AudioManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.CameraShakeManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.GameplayManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.LoadingManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.ScoreManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.UIManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.UserPreferencesManager
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.ActorManager
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

internal class SpaceSquadronGameStateHolderImpl(
    webRootPathName: String,
    isLoggingEnabled: Boolean,
) : SpaceSquadronGameStateHolder {

    private val sharedMusicManager = MusicManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    private val sharedSoundManager = SoundManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    private val sharedSpriteManager = SpriteManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    val backgroundLoadingManager = LoadingManager(
        webRootPathName = webRootPathName,
    )
    private val backgroundStateManager = StateManager.newInstance()
    private val backgroundShaderManager = ShaderManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG_BACKGROUND,
    )
    private val backgroundActorManager = ActorManager.newInstance(
        initialActors = listOf(GalaxyShader()),
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG_BACKGROUND,
    )
    val backgroundKubriko = Kubriko.newInstance(
        backgroundStateManager,
        sharedMusicManager,
        sharedSoundManager,
        sharedSpriteManager,
        backgroundShaderManager,
        backgroundLoadingManager,
        backgroundActorManager,
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG_BACKGROUND,
    )
    private val viewportManager = ViewportManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    val cameraShakeManager = CameraShakeManager()
    val stateManager = StateManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    private val persistenceManager = PersistenceManager.newInstance(
        fileName = "kubrikoSpaceSquadron",
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    private val scoreManager = ScoreManager(
        persistenceManager = persistenceManager,
    )
    val userPreferencesManager = UserPreferencesManager(
        persistenceManager = persistenceManager,
    )
    val audioManager = AudioManager(
        stateManager = stateManager,
        userPreferencesManager = userPreferencesManager,
        webRootPathName = webRootPathName,
    )
    private val particleManager = ParticleManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    val gameplayManager = GameplayManager(
        backgroundStateManager = backgroundStateManager,
    )
    private val actorManager = ActorManager.newInstance(
        shouldUpdateActorsWhileNotRunning = true, // To ensure proper scaling during resize events while paused
        shouldPutFarAwayActorsToSleep = false,
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    val uiManager = UIManager(
        stateManager = stateManager,
    )
    private val collisionManager = CollisionManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    private val keyboardInputManager = KeyboardInputManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    private val pointerInputManager = PointerInputManager.newInstance(
        isActiveAboveViewport = true,
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    private val _kubriko = MutableStateFlow(
        Kubriko.newInstance(
            actorManager,
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
            cameraShakeManager,
            scoreManager,
            uiManager,
            audioManager,
            isLoggingEnabled = isLoggingEnabled,
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
    ) = backgroundLoadingManager.isLoadingDone.also {
        if (it) {
            if (stateManager.isRunning.value && !gameplayManager.isGameOver.value) {
                gameplayManager.pauseGame()
            } else if (uiManager.isInfoDialogVisible.value) {
                uiManager.toggleInfoDialogVisibility()
            } else if (gameplayManager.isGameStarted && !uiManager.isCloseConfirmationDialogVisible.value) {
                gameplayManager.playGame()
            } else if (isInFullscreenMode) {
                audioManager.playButtonToggleSoundEffect()
                onFullscreenModeToggled()
            } else {
                uiManager.toggleCloseConfirmationDialogVisibility()
            }
        }
    }

    override fun dispose() {
        backgroundKubriko.dispose()
        kubriko.value.dispose()
    }
}

private const val LOG_TAG = "SS"
private const val LOG_TAG_BACKGROUND = "$LOG_TAG-Background"