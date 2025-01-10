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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
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
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.AudioManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.BackgroundAnimationManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.GameplayManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.LoadingManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.UIManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers.UserPreferencesManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui.SpaceSquadronMenuOverlay
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui.SpaceSquadronTheme
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.shaders.ShaderManager
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.shared.ExampleStateHolder

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
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.BottomStart).size(24.dp),
                strokeWidth = 3.dp,
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
                isVisible = !stateHolder.stateManager.isRunning.collectAsState().value,
                shouldShowInfoText = stateHolder.uiManager.isInfoDialogVisible.collectAsState().value,
                onPlayButtonPressed = stateHolder.gameManager::playGame,
                onPauseButtonPressed = stateHolder.gameManager::pauseGame,
                onInfoButtonPressed = {
                    stateHolder.audioManager.playButtonToggleSoundEffect()
                    stateHolder.uiManager.toggleInfoDialogVisibility()
                },
                areSoundEffectsEnabled = stateHolder.userPreferencesManager.areSoundEffectsEnabled.collectAsState().value,
                onSoundEffectsToggled = stateHolder.userPreferencesManager::onAreSoundEffectsEnabledChanged,
                isMusicEnabled = stateHolder.userPreferencesManager.isMusicEnabled.collectAsState().value,
                onMusicToggled = stateHolder.userPreferencesManager::onIsMusicEnabledChanged,
                isInFullscreenMode = isInFullscreenMode,
                onFullscreenModeToggled = {
                    stateHolder.audioManager.playButtonToggleSoundEffect()
                    onFullscreenModeToggled()
                },
                onButtonHover = stateHolder.audioManager::playButtonHoverSoundEffect,
            )
        }
    }
}

sealed interface SpaceSquadronGameStateHolder : ExampleStateHolder

fun createSpaceSquadronGameStateHolder(): SpaceSquadronGameStateHolder = SpaceSquadronGameStateHolderImpl()

private class SpaceSquadronGameStateHolderImpl : SpaceSquadronGameStateHolder {
    val stateManager = StateManager.newInstance(shouldAutoStart = false)
    private val persistenceManager = PersistenceManager.newInstance(fileName = "kubrikoSpaceSquadron")
    val userPreferencesManager = UserPreferencesManager(persistenceManager)
    val loadingManager = LoadingManager()
    private val musicManager = MusicManager.newInstance()
    private val soundManager = SoundManager.newInstance()
    val audioManager = AudioManager(stateManager, userPreferencesManager)
    val gameManager = GameplayManager()
    val spriteManager = SpriteManager.newInstance()
    val uiManager = UIManager(stateManager)
    val backgroundKubriko = Kubriko.newInstance(
        ShaderManager.newInstance(),
        musicManager,
        soundManager,
        spriteManager,
        loadingManager,
        BackgroundAnimationManager()
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
        uiManager,
        audioManager,
    )

    override fun dispose() {
        kubriko.dispose()
        backgroundKubriko.dispose()
    }
}

internal val ViewportHeight = 1280.sceneUnit