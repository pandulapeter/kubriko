package com.pandulapeter.kubriko.gameWallbreaker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.audioPlayback.AudioPlaybackManager
import com.pandulapeter.kubriko.collision.CollisionManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.WallbreakerGameManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.AudioManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.ScoreManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.managers.UserPreferencesManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ui.GameOverlay
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ui.PauseMenuOverlay
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.persistence.PersistenceManager
import com.pandulapeter.kubriko.shader.ShaderManager

/**
 * Music: https://opengameart.org/content/cyberpunk-moonlight-sonata
 */
@Composable
fun WallbreakerGame(
    modifier: Modifier = Modifier,
    stateHolder: WallbreakerGameStateHolder = createWallbreakerGameStateHolder(),
) {
    stateHolder as WallbreakerGameStateHolderImpl
    Box {
        KubrikoViewport(
            modifier = modifier.background(Color.Black),
            kubriko = stateHolder.kubriko,
        ) {
            PauseMenuOverlay(
                isGameRunning = stateHolder.stateManager.isRunning.collectAsState().value,
                onResumeButtonPressed = { stateHolder.stateManager.updateIsRunning(true) },
                areSoundEffectsEnabled = stateHolder.userPreferencesManager.areSoundEffectsEnabled.collectAsState().value,
                onSoundEffectsToggled = stateHolder.userPreferencesManager::onAreSoundEffectsEnabledChanged,
                isMusicEnabled = stateHolder.userPreferencesManager.isMusicEnabled.collectAsState().value,
                onMusicToggled = stateHolder.userPreferencesManager::onIsMusicEnabledChanged,
            )
            GameOverlay(
                isGameRunning = stateHolder.stateManager.isRunning.collectAsState().value,
                score = stateHolder.scoreManager.score.collectAsState().value,
                highScore = stateHolder.scoreManager.highScore.collectAsState().value,
                onPauseButtonPressed = { stateHolder.stateManager.updateIsRunning(false) },
            )
        }
    }
}

sealed interface WallbreakerGameStateHolder {
    fun dispose()
}

fun createWallbreakerGameStateHolder(): WallbreakerGameStateHolder = WallbreakerGameStateHolderImpl()

internal class WallbreakerGameStateHolderImpl : WallbreakerGameStateHolder {
    val stateManager = StateManager.newInstance(shouldAutoStart = false)
    private val audioPlaybackManager = AudioPlaybackManager.newInstance()
    private val persistenceManager = PersistenceManager.newInstance(fileName = "kubrikoWallbreaker")
    val scoreManager = ScoreManager(persistenceManager)
    val userPreferencesManager = UserPreferencesManager(persistenceManager)
    val kubriko = Kubriko.newInstance(
        stateManager,
        ViewportManager.newInstance(
            aspectRatioMode = ViewportManager.AspectRatioMode.Fixed(
                ratio = 1f,
                defaultWidth = 1200.sceneUnit,
            )
        ),
        CollisionManager.newInstance(),
        ShaderManager.newInstance(),
        KeyboardInputManager.newInstance(),
        persistenceManager,
        scoreManager,
        userPreferencesManager,
        audioPlaybackManager,
        AudioManager(audioPlaybackManager, userPreferencesManager),
        WallbreakerGameManager(),
    )

    override fun dispose() = kubriko.dispose()
}