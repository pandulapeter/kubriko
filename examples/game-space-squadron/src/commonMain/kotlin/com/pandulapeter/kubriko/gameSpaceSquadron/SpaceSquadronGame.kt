package com.pandulapeter.kubriko.gameSpaceSquadron

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.audioPlayback.AudioPlaybackManager
import com.pandulapeter.kubriko.collision.CollisionManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.SpaceSquadronAudioManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.SpaceSquadronBackgroundManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.SpaceSquadronGameManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.SpaceSquadronUserPreferencesManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui.SpaceSquadronPauseMenuOverlay
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui.SpaceSquadronTheme
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.shaders.ShaderManager
import com.pandulapeter.kubriko.shared.ExampleStateHolder
import com.pandulapeter.kubriko.sprites.SpriteManager

/**
 * Music: https://freesound.org/people/Andrewkn/sounds/474864/
 */
@Composable
fun SpaceSquadronGame(
    stateHolder: SpaceSquadronGameStateHolder = createSpaceSquadronGameStateHolder(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    isInFullscreenMode: Boolean? = null,
    onFullscreenModeToggled: () -> Unit = {},
) = SpaceSquadronTheme {
    stateHolder as SpaceSquadronGameStateHolderImpl
    // TODO: Move background into the Composable function of a Manager class
    KubrikoViewport(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        kubriko = stateHolder.backgroundKubriko,
    )
    KubrikoViewport(
        kubriko = stateHolder.kubriko,
        windowInsets = windowInsets,
    )
    SpaceSquadronPauseMenuOverlay(
        modifier = Modifier.windowInsetsPadding(windowInsets),
        isGameRunning = false,
        onNewGameButtonPressed = { stateHolder.stateManager.updateIsRunning(true) },
        areSoundEffectsEnabled = stateHolder.userPreferencesManager.areSoundEffectsEnabled.collectAsState().value,
        onSoundEffectsToggled = stateHolder.userPreferencesManager::onAreSoundEffectsEnabledChanged,
        isMusicEnabled = stateHolder.userPreferencesManager.isMusicEnabled.collectAsState().value,
        onMusicToggled = stateHolder.userPreferencesManager::onIsMusicEnabledChanged,
        isInFullscreenMode = isInFullscreenMode,
        onFullscreenModeToggled = onFullscreenModeToggled,
    )
}

sealed interface SpaceSquadronGameStateHolder : ExampleStateHolder

fun createSpaceSquadronGameStateHolder(): SpaceSquadronGameStateHolder = SpaceSquadronGameStateHolderImpl()

private class SpaceSquadronGameStateHolderImpl : SpaceSquadronGameStateHolder {
    private val audioPlaybackManager = AudioPlaybackManager.newInstance()
    val stateManager = StateManager.newInstance(shouldAutoStart = false)
    private val persistenceManager = PersistenceManager.newInstance(fileName = "kubrikoSpaceSquadron")
    val userPreferencesManager = SpaceSquadronUserPreferencesManager(persistenceManager)
    val backgroundKubriko = Kubriko.newInstance(
        ShaderManager.newInstance(),
        SpaceSquadronBackgroundManager()
    )
    val kubriko = Kubriko.newInstance(
        stateManager,
        ViewportManager.newInstance(
            aspectRatioMode = ViewportManager.AspectRatioMode.FitVertical(
                height = ViewportHeight,
            )
        ),
        CollisionManager.newInstance(),
        KeyboardInputManager.newInstance(),
        PointerInputManager.newInstance(isActiveAboveViewport = true),
        SpaceSquadronGameManager(),
        audioPlaybackManager,
        persistenceManager,
        userPreferencesManager,
        SpriteManager.newInstance(),
        SpaceSquadronAudioManager(stateManager, audioPlaybackManager, userPreferencesManager),
    )

    override fun dispose() = kubriko.dispose()
}

internal val ViewportHeight = 1280.sceneUnit