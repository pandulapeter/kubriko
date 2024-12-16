package com.pandulapeter.kubriko.gameWallbreaker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.audioPlayback.AudioPlaybackManager
import com.pandulapeter.kubriko.collision.CollisionManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.WallbreakerAudioManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.WallbreakerBackgroundManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.WallbreakerGameManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.WallbreakerScoreManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.WallbreakerUserPreferencesManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ui.GameOverlay
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ui.PauseMenuOverlay
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shared.ExampleStateHolder

/**
 * Music: https://opengameart.org/content/cyberpunk-moonlight-sonata
 */
@Composable
fun WallbreakerGame(
    modifier: Modifier = Modifier,
    stateHolder: WallbreakerGameStateHolder = createWallbreakerGameStateHolder(),
) {
    stateHolder as WallbreakerGameStateHolderImpl
    KubrikoViewport(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        kubriko = stateHolder.backgroundKubriko,
    ) {
        KubrikoViewport(
            modifier = modifier.background(Color.Black),
            kubriko = stateHolder.kubriko,
        ) {
            PauseMenuOverlay(
                isGameRunning = stateHolder.stateManager.isRunning.collectAsState().value,
                shouldShowResumeButton = !stateHolder.wallbreakerGameManager.isGameOver.collectAsState().value,
                onResumeButtonPressed = stateHolder.wallbreakerGameManager::resumeGame,
                onRestartButtonPressed = stateHolder.wallbreakerGameManager::restartGame,
                areSoundEffectsEnabled = stateHolder.wallbreakerUserPreferencesManager.areSoundEffectsEnabled.collectAsState().value,
                onSoundEffectsToggled = stateHolder.wallbreakerUserPreferencesManager::onAreSoundEffectsEnabledChanged,
                isMusicEnabled = stateHolder.wallbreakerUserPreferencesManager.isMusicEnabled.collectAsState().value,
                onMusicToggled = stateHolder.wallbreakerUserPreferencesManager::onIsMusicEnabledChanged,
            )
            GameOverlay(
                isGameRunning = stateHolder.stateManager.isRunning.collectAsState().value,
                score = stateHolder.wallbreakerScoreManager.score.collectAsState().value,
                highScore = stateHolder.wallbreakerScoreManager.highScore.collectAsState().value,
                onPauseButtonPressed = stateHolder.wallbreakerGameManager::pauseGame,
            )
        }
    }
}

sealed interface WallbreakerGameStateHolder : ExampleStateHolder

fun createWallbreakerGameStateHolder(): WallbreakerGameStateHolder = WallbreakerGameStateHolderImpl()

private class WallbreakerGameStateHolderImpl : WallbreakerGameStateHolder {
    val stateManager = StateManager.newInstance(shouldAutoStart = false)
    private val audioPlaybackManager = AudioPlaybackManager.newInstance()
    private val persistenceManager = PersistenceManager.newInstance(fileName = "kubrikoWallbreaker")
    val wallbreakerScoreManager = WallbreakerScoreManager(persistenceManager)
    val wallbreakerUserPreferencesManager = WallbreakerUserPreferencesManager(persistenceManager)
    val wallbreakerGameManager = WallbreakerGameManager(stateManager)
    val backgroundKubriko = Kubriko.newInstance(
        ShaderManager.newInstance(),
        WallbreakerBackgroundManager(),
    )
    val kubriko = Kubriko.newInstance(
        stateManager,
        ViewportManager.newInstance(
            aspectRatioMode = ViewportManager.AspectRatioMode.Fixed(
                ratio = 1f,
                width = 1200.sceneUnit,
            )
        ),
        CollisionManager.newInstance(),
        ShaderManager.newInstance(),
        KeyboardInputManager.newInstance(),
        PointerInputManager.newInstance(isActiveAboveViewport = true),
        persistenceManager,
        wallbreakerScoreManager,
        wallbreakerUserPreferencesManager,
        audioPlaybackManager,
        WallbreakerAudioManager(stateManager, audioPlaybackManager, wallbreakerUserPreferencesManager),
        wallbreakerGameManager,
    )

    override fun dispose() = kubriko.dispose()
}