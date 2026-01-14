/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameBlockysJourney.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.actors.Block
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.actors.Blocky
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.managers.AudioManager
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.managers.GameplayManager
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.managers.LoadingManager
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.managers.UIManager
import com.pandulapeter.kubriko.gameBlockysJourney.implementation.managers.UserPreferencesManager
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.shaders.ShaderManager
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.SceneSize
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

sealed interface BlockysJourneyGameStateHolder : StateHolder

internal class BlockysJourneyGameStateHolderImpl(
    webRootPathName: String,
    val isSceneEditorEnabled: Boolean,
    isLoggingEnabled: Boolean,
) : BlockysJourneyGameStateHolder {

    private val json = Json { ignoreUnknownKeys = true }
    val backgroundSerializationManager = EditableMetadata.newSerializationManagerInstance(
        EditableMetadata(
            typeId = "blocky",
            deserializeState = { serializedState -> json.decodeFromString<Blocky.State>(serializedState) },
            instantiate = { Blocky.State(body = BoxBody(initialPosition = it, initialSize = SceneSize(256.sceneUnit, 256.sceneUnit))) }
        ),
        EditableMetadata(
            typeId = "block",
            deserializeState = { serializedState -> json.decodeFromString<Block.State>(serializedState) },
            instantiate = { Block.State(body = BoxBody(initialPosition = it, initialSize = SceneSize(512.sceneUnit, 512.sceneUnit))) }
        ),
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    val customManagersForSceneEditor by lazy {
        listOf(
            sharedSpriteManager,
        )
    }
    private val sharedMusicManager by lazy {
        MusicManager.newInstance(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val sharedSoundManager by lazy {
        SoundManager.newInstance(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val sharedSpriteManager by lazy {
        SpriteManager.newInstance(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val persistenceManager by lazy {
        PersistenceManager.newInstance(
            fileName = "kubrikoBlockysJourney",
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val viewportManager by lazy {
        ViewportManager.newInstance(
            aspectRatioMode = ViewportManager.AspectRatioMode.FitVertical(1440.sceneUnit),
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val shaderManager by lazy {
        ShaderManager.newInstance(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        )
    }
    val sharedLoadingManager by lazy {
        LoadingManager(
            webRootPathName = webRootPathName,
        )
    }
    val stateManager by lazy {
        StateManager.newInstance(
            shouldAutoStart = false,
            isLoggingEnabled = isLoggingEnabled,
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
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val pointerInputManager by lazy {
        PointerInputManager.newInstance(
            isActiveAboveViewport = true,
            isLoggingEnabled = isLoggingEnabled,
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
            sharedLoadingManager,
            backgroundSerializationManager,
            isLoggingEnabled = isLoggingEnabled,
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
                sharedLoadingManager,
                stateManager,
                shaderManager,
                viewportManager,
                keyboardInputManager,
                pointerInputManager,
                audioManager,
                gameplayManager,
                uiManager,
                isLoggingEnabled = isLoggingEnabled,
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
    ) = sharedLoadingManager.isLoadingDone.also {
        if (it) {
            if (stateManager.isRunning.value) {
                audioManager.playButtonToggleSoundEffect()
                stateManager.updateIsRunning(false)
            } else if (uiManager.isInfoDialogVisible.value) {
                audioManager.playButtonToggleSoundEffect()
                uiManager.toggleInfoDialogVisibility()
//            } else if (!uiManager.isCloseConfirmationDialogVisible.value) {
//                audioManager.playButtonToggleSoundEffect()
//                stateManager.updateIsRunning(true)
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

private const val LOG_TAG = "BJ"
private const val LOG_TAG_BACKGROUND = "$LOG_TAG-Background"