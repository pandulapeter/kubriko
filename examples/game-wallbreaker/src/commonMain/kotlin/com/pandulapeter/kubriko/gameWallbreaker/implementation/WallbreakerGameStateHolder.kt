/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameWallbreaker.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.collision.CollisionManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.actors.FogShader
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.AudioManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.GameplayManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.LoadingManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.ScoreManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.UIManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.UserPreferencesManager
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.shaders.ShaderManager
import com.pandulapeter.kubriko.shared.StateHolder
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface WallbreakerGameStateHolder : StateHolder

internal class WallbreakerGameStateHolderImpl(
    webRootPathName: String,
    isLoggingEnabled: Boolean,
) : WallbreakerGameStateHolder {
    private val sharedMusicManager = MusicManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    private val sharedSoundManager = SoundManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    private val backgroundShaderManager = ShaderManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG_BACKGROUND,
    )
    private val backgroundActorManager = ActorManager.newInstance(
        initialActors = listOf(FogShader()),
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG_BACKGROUND,
    )
    val backgroundLoadingManager = LoadingManager(
        webRootPathName = webRootPathName,
    )
    val backgroundKubriko = Kubriko.newInstance(
        sharedMusicManager,
        sharedSoundManager,
        backgroundShaderManager,
        backgroundLoadingManager,
        backgroundActorManager,
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG_BACKGROUND,
    )
    val stateManager = StateManager.newInstance(
        shouldAutoStart = false,
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    private val viewportManager = ViewportManager.newInstance(
        aspectRatioMode = ViewportManager.AspectRatioMode.Fixed(
            ratio = 1f,
            width = 1200.sceneUnit,
        ),
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    private val persistenceManager = PersistenceManager.newInstance(
        fileName = "kubrikoWallbreaker",
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    val scoreManager = ScoreManager(
        persistenceManager = persistenceManager,
    )
    val shaderManager = ShaderManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    val userPreferencesManager = UserPreferencesManager(
        persistenceManager = persistenceManager,
    )
    val gameplayManager = GameplayManager(
        stateManager = stateManager,
    )
    val audioManager = AudioManager(
        stateManager = stateManager,
        userPreferencesManager = userPreferencesManager,
        webRootPathName = webRootPathName,
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
            sharedMusicManager,
            sharedSoundManager,
            stateManager,
            viewportManager,
            collisionManager,
            shaderManager,
            keyboardInputManager,
            pointerInputManager,
            persistenceManager,
            scoreManager,
            userPreferencesManager,
            audioManager,
            gameplayManager,
            uiManager,
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
            if (stateManager.isRunning.value) {
                gameplayManager.pauseGame()
            } else if (uiManager.isInfoDialogVisible.value) {
                uiManager.toggleInfoDialogVisibility()
            } else if (gameplayManager.isGameStarted) {
                gameplayManager.resumeGame()
            } else if (isInFullscreenMode) {
                audioManager.playClickSoundEffect()
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

private const val LOG_TAG = "WB"
private const val LOG_TAG_BACKGROUND = "$LOG_TAG-Background"