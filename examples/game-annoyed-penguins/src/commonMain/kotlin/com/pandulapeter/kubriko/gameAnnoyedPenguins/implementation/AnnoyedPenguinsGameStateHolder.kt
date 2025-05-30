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
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.collision.CollisionManager
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.DestructibleBlock
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.FogShader
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.Ground
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.Star
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.slingshot.Slingshot
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.managers.AudioManager
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.managers.GameplayManager
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.managers.LoadingManager
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.managers.UIManager
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.managers.UserPreferencesManager
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import com.pandulapeter.kubriko.physics.PhysicsManager
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

sealed interface AnnoyedPenguinsGameStateHolder : StateHolder

internal class AnnoyedPenguinsGameStateHolderImpl(
    webRootPathName: String,
    val isSceneEditorEnabled: Boolean,
    isLoggingEnabled: Boolean,
    isForSceneEditor: Boolean,
) : AnnoyedPenguinsGameStateHolder {

    private val json = Json { ignoreUnknownKeys = true }
    val serializationManager = EditableMetadata.newSerializationManagerInstance(
        EditableMetadata(
            typeId = "block",
            deserializeState = { serializedState -> json.decodeFromString<DestructibleBlock.State>(serializedState) },
            instantiate = { DestructibleBlock.State(body = BoxBody(initialPosition = it, initialSize = SceneSize(128.sceneUnit, 128.sceneUnit))) },
        ),
        EditableMetadata(
            typeId = "ground",
            deserializeState = { serializedState -> json.decodeFromString<Ground.State>(serializedState) },
            instantiate = { Ground.State(body = BoxBody(initialPosition = it, initialSize = SceneSize(128.sceneUnit, 128.sceneUnit))) },
        ),
        EditableMetadata(
            typeId = "slingshot",
            deserializeState = { serializedState -> json.decodeFromString<Slingshot.State>(serializedState) },
            instantiate = { Slingshot.State(body = BoxBody(initialPosition = it, initialSize = SceneSize(422.sceneUnit, 924.sceneUnit))) },
        ),
        EditableMetadata(
            typeId = "star",
            deserializeState = { serializedState -> json.decodeFromString<Star.State>(serializedState) },
            instantiate = { Star.State(body = BoxBody(initialPosition = it, initialSize = SceneSize(256.sceneUnit, 256.sceneUnit))) },
        ),
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG,
    )
    val customManagersForSceneEditor by lazy {
        listOf(
            sharedSpriteManager,
            sharedMusicManager,
            sharedSoundManager,
            audioManager,
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
            fileName = "kubrikoAnnoyedPenguins",
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val collisionManager by lazy {
        CollisionManager.newInstance(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        )
    }
    val viewportManager by lazy {
        ViewportManager.newInstance(
            aspectRatioMode = ViewportManager.AspectRatioMode.FitVertical(1440.sceneUnit),
            minimumScaleFactor = 0.25f,
            maximumScaleFactor = 1f,
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        )
    }
    private val backgroundShaderManager by lazy {
        ShaderManager.newInstance(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG_BACKGROUND,
        )
    }
    private val shaderManager by lazy {
        ShaderManager.newInstance(
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG_BACKGROUND,
        )
    }
    val backgroundLoadingManager by lazy {
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
    private val backgroundActorManager by lazy {
        ActorManager.newInstance(
            initialActors = listOf(FogShader()),
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG_BACKGROUND,
        )
    }
    private val actorManager by lazy {
        ActorManager.newInstance(
            shouldPutFarAwayActorsToSleep = false,
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG,
        )
    }
    val sharedUserPreferencesManager by lazy {
        UserPreferencesManager(persistenceManager)
    }
    val audioManager by lazy {
        AudioManager(
            isForSceneEditor = isForSceneEditor,
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
    private val physicsManager by lazy {
        PhysicsManager.newInstance(
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
    val gameplayManager by lazy {
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
            backgroundActorManager,
            backgroundLoadingManager,
            isLoggingEnabled = isLoggingEnabled,
            instanceNameForLogging = LOG_TAG_BACKGROUND,
        )
    }
    private val _kubriko by lazy {
        MutableStateFlow(
            Kubriko.newInstance(
                actorManager,
                persistenceManager,
                sharedUserPreferencesManager,
                sharedMusicManager,
                sharedSoundManager,
                sharedSpriteManager,
                collisionManager,
                stateManager,
                viewportManager,
                physicsManager,
                keyboardInputManager,
                pointerInputManager,
                audioManager,
                serializationManager,
                gameplayManager,
                shaderManager,
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
    ) = backgroundLoadingManager.isLoadingDone.also {
        if (it) {
            if (stateManager.isRunning.value) {
                audioManager.playButtonToggleSoundEffect()
                stateManager.updateIsRunning(false)
            } else if (uiManager.isInfoDialogVisible.value) {
                audioManager.playButtonToggleSoundEffect()
                uiManager.toggleInfoDialogVisibility()
            } else if (gameplayManager.currentLevel.value != null && !uiManager.isCloseConfirmationDialogVisible.value) {
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