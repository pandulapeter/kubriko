package com.pandulapeter.kubriko.gameSpaceSquadron

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.collision.CollisionManager
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.SpaceSquadronAudioManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.SpaceSquadronBackgroundManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.SpaceSquadronGameManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.SpaceSquadronLoadingManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.SpaceSquadronUIManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.SpaceSquadronUserPreferencesManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui.SpaceSquadronMenuOverlay
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui.SpaceSquadronTheme
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.shaders.ShaderManager
import com.pandulapeter.kubriko.shared.ExampleStateHolder
import com.pandulapeter.kubriko.shared.ui.LoadingIndicator
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
    val isGameLoaded = stateHolder.loadingManager.isGameLoaded()
    KubrikoViewport(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        kubriko = stateHolder.backgroundKubriko,
    )
    AnimatedVisibility(
        visible = !isGameLoaded,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .windowInsetsPadding(windowInsets)
                .padding(16.dp),
        ) {
            LoadingIndicator(
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
    AnimatedVisibility(
        visible = isGameLoaded,
        enter = fadeIn() + scaleIn(initialScale = 0.88f),
        exit = scaleOut(targetScale = 0.88f) + fadeOut(),
    ) {
        KubrikoViewport(
            kubriko = stateHolder.kubriko,
            windowInsets = windowInsets,
        )
        AnimatedVisibility(
            visible = isGameLoaded,
            enter = fadeIn() + scaleIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            SpaceSquadronMenuOverlay(
                modifier = Modifier.windowInsetsPadding(windowInsets),
                isGameRunning = stateHolder.stateManager.isRunning.collectAsState().value,
                onPlayButtonPressed = stateHolder.gameManager::playGame,
                onPauseButtonPressed = stateHolder.gameManager::pauseGame,
                onInfoButtonPressed = { stateHolder.audioManager.playButtonToggleSoundEffect() }, // TODO
                areSoundEffectsEnabled = stateHolder.userPreferencesManager.areSoundEffectsEnabled.collectAsState().value,
                onSoundEffectsToggled = stateHolder.userPreferencesManager::onAreSoundEffectsEnabledChanged,
                isMusicEnabled = stateHolder.userPreferencesManager.isMusicEnabled.collectAsState().value,
                onMusicToggled = stateHolder.userPreferencesManager::onIsMusicEnabledChanged,
                isInFullscreenMode = isInFullscreenMode,
                onFullscreenModeToggled = {
                    stateHolder.audioManager.playButtonToggleSoundEffect()
                    onFullscreenModeToggled()
                },
            )
        }
    }
}

sealed interface SpaceSquadronGameStateHolder : ExampleStateHolder

fun createSpaceSquadronGameStateHolder(): SpaceSquadronGameStateHolder = SpaceSquadronGameStateHolderImpl()

private class SpaceSquadronGameStateHolderImpl : SpaceSquadronGameStateHolder {
    val stateManager = StateManager.newInstance(shouldAutoStart = false)
    private val persistenceManager = PersistenceManager.newInstance(fileName = "kubrikoSpaceSquadron")
    val userPreferencesManager = SpaceSquadronUserPreferencesManager(persistenceManager)
    val loadingManager = SpaceSquadronLoadingManager()
    private val musicManager = MusicManager.newInstance()
    private val soundManager = SoundManager.newInstance()
    val audioManager = SpaceSquadronAudioManager(stateManager, userPreferencesManager)
    val gameManager = SpaceSquadronGameManager()
    val spriteManager = SpriteManager.newInstance()
    val backgroundKubriko = Kubriko.newInstance(
        ShaderManager.newInstance(),
        musicManager,
        soundManager,
        spriteManager,
        loadingManager,
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
        persistenceManager,
        userPreferencesManager,
        musicManager,
        soundManager,
        spriteManager,
        gameManager,
        SpaceSquadronUIManager(stateManager),
        audioManager,
    )

    override fun dispose() {
        kubriko.dispose()
        backgroundKubriko.dispose()
    }
}

internal val ViewportHeight = 1280.sceneUnit