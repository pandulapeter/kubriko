/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.managers.AudioManager
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.managers.BackgroundAnimationManager
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.managers.GameplayManager
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.managers.LoadingManager
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.managers.UIManager
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.managers.UserPreferencesManager
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.particles.ParticleManager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import com.pandulapeter.kubriko.physics.PhysicsManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.shaders.ShaderManager
import com.pandulapeter.kubriko.shared.StateHolder
import com.pandulapeter.kubriko.sprites.SpriteManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

sealed interface AnnoyedPenguinsGameStateHolder : StateHolder

internal class AnnoyedPenguinsGameStateHolderImpl : AnnoyedPenguinsGameStateHolder {

    private val json = Json { ignoreUnknownKeys = true }
    val serializationManager = EditableMetadata.newSerializationManagerInstance(
//        EditableMetadata(
//            typeId = "penguin",
//            deserializeState = { serializedState -> json.decodeFromString<StaticBox.State>(serializedState) },
//            instantiate = { StaticBox.State(body = RectangleBody(initialPosition = it, initialSize = SceneSize(100.sceneUnit, 100.sceneUnit))) },
//        ),
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
    private val sharedPersistenceManager by lazy {
        PersistenceManager.newInstance(
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val viewportManager by lazy {
        ViewportManager.newInstance(
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
        LoadingManager()
    }
    val stateManager by lazy {
        StateManager.newInstance(
            shouldAutoStart = false,
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val backgroundAnimationManager by lazy {
        BackgroundAnimationManager()
    }
    val sharedUserPreferencesManager by lazy {
        UserPreferencesManager()
    }
    val sharedAudioManager by lazy {
        AudioManager()
    }
    private val particleManager by lazy {
        ParticleManager.newInstance(
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val keyboardInputManager by lazy {
        KeyboardInputManager.newInstance(
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val physicsManager by lazy {
        PhysicsManager.newInstance(
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
    val gameplayManager by lazy {
        GameplayManager()
    }
    val uiManager by lazy {
        UIManager()
    }
    val backgroundKubriko by lazy {
        Kubriko.newInstance(
            sharedPersistenceManager,
            sharedUserPreferencesManager,
            sharedAudioManager,
            sharedMusicManager,
            sharedSoundManager,
            sharedSpriteManager,
            backgroundShaderManager,
            backgroundAnimationManager,
            backgroundLoadingManager,
            isLoggingEnabled = true,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val _kubriko by lazy {
        MutableStateFlow(
            Kubriko.newInstance(
                sharedMusicManager,
                sharedSoundManager,
                sharedSpriteManager,
                stateManager,
                viewportManager,
                physicsManager,
                keyboardInputManager,
                pointerInputManager,
                sharedPersistenceManager,
                sharedUserPreferencesManager,
                particleManager,
                sharedAudioManager,
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

    override fun stopMusic() = sharedAudioManager.stopMusicBeforeDispose()

    override fun navigateBack() = (stateManager.isRunning.value || uiManager.isInfoDialogVisible.value).also {
        if (stateManager.isRunning.value) {
            sharedAudioManager.playButtonToggleSoundEffect()
            stateManager.updateIsRunning(false)
        } else if (uiManager.isInfoDialogVisible.value) {
            sharedAudioManager.playButtonToggleSoundEffect()
            uiManager.toggleInfoDialogVisibility()
        }
    }

    override fun dispose() {
        backgroundKubriko.dispose()
        kubriko.value.dispose()
    }
}

private const val LOG_TAG = "AP"