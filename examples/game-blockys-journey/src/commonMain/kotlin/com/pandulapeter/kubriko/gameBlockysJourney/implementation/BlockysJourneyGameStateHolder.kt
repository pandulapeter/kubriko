/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameBlockysJourney.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.actors.BackgroundShader
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.managers.AudioManager
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.managers.GameplayManager
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.managers.LoadingManager
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.managers.UIManager
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.managers.UserPreferencesManager
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.shaders.ShaderManager
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.sprites.SpriteManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

sealed interface BlockysJourneyGameStateHolder : StateHolder

internal class BlockysJourneyGameStateHolderImpl(
    webRootPathName: String,
    val isSceneEditorEnabled: Boolean,
) : BlockysJourneyGameStateHolder {

    private val json = Json { ignoreUnknownKeys = true }
    val serializationManager = EditableMetadata.newSerializationManagerInstance(
        isLoggingEnabled = true,
        instanceNameForLogging = LOG_TAG,
    )
    private val sharedMusicManager by lazy {
        MusicManager.newInstance(
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val sharedSoundManager by lazy {
        SoundManager.newInstance(
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val sharedSpriteManager by lazy {
        SpriteManager.newInstance(
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val persistenceManager by lazy {
        PersistenceManager.newInstance(
            fileName = "kubrikoBlockysJourney",
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val viewportManager by lazy {
        ViewportManager.newInstance(
            aspectRatioMode = ViewportManager.AspectRatioMode.FitVertical(1440.sceneUnit),
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val backgroundShaderManager by lazy {
        ShaderManager.newInstance(
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }
    val backgroundLoadingManager by lazy {
        LoadingManager(
            webRootPathName = webRootPathName,
        )
    }
    val backgroundActorManager by lazy {
        ActorManager.newInstance(
            listOf(BackgroundShader()),
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }
    val stateManager by lazy {
        StateManager.newInstance(
            shouldAutoStart = false,
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }
    val sharedUserPreferencesManager by lazy {
        UserPreferencesManager(persistenceManager)
    }
    val audioManager by lazy {
        AudioManager(
            stateManager = stateManager,
            userPreferencesManager = sharedUserPreferencesManager,
            webRootPathName = webRootPathName,
        )
    }
    private val keyboardInputManager by lazy {
        KeyboardInputManager.newInstance(
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val pointerInputManager by lazy {
        PointerInputManager.newInstance(
            isActiveAboveViewport = true,
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val gameplayManager by lazy {
        GameplayManager()
    }
    val uiManager by lazy {
        UIManager()
    }
    val backgroundKubriko by lazy {
        Kubriko.newInstance(
            sharedMusicManager,
            sharedSoundManager,
            sharedSpriteManager,
            backgroundShaderManager,
            backgroundLoadingManager,
            backgroundActorManager,
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG_BACKGROUND,
        )
    }
    private val _kubriko by lazy {
        MutableStateFlow(
            Kubriko.newInstance(
                persistenceManager,
                sharedUserPreferencesManager,
                sharedMusicManager,
                sharedSoundManager,
                sharedSpriteManager,
                stateManager,
                viewportManager,
                keyboardInputManager,
                pointerInputManager,
                audioManager,
                serializationManager,
                gameplayManager,
                uiManager,
                isLoggingEnabled = true,
                instanceNameForLogging = LOG_TAG,
            )
        )
    }
    override val kubriko by lazy {
        _kubriko.asStateFlow()
    }

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
                audioManager.playButtonToggleSoundEffect()
                stateManager.updateIsRunning(false)
            } else if (uiManager.isInfoDialogVisible.value) {
                audioManager.playButtonToggleSoundEffect()
                uiManager.toggleInfoDialogVisibility()
            } else if (!uiManager.isCloseConfirmationDialogVisible.value) {
                audioManager.playButtonToggleSoundEffect()
                stateManager.updateIsRunning(true)
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

private const val LOG_TAG = "AP"
private const val LOG_TAG_BACKGROUND = "$LOG_TAG-Background"