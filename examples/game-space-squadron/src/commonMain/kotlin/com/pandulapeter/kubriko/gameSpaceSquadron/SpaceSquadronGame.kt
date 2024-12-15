package com.pandulapeter.kubriko.gameSpaceSquadron

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.collision.CollisionManager
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.keyboardInput.KeyboardInputManager
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shared.ExampleStateHolder

@Composable
fun SpaceSquadronGame(
    modifier: Modifier = Modifier,
    stateHolder: SpaceSquadronGameStateHolder = createSpaceSquadronGameStateHolder(),
) {
    stateHolder as SpaceSquadronGameStateHolderImpl
    Box {
        KubrikoViewport(
            modifier = modifier.background(Color.Black),
            kubriko = stateHolder.kubriko,
        ) {
//            PauseMenuOverlay(
//                isGameRunning = stateHolder.stateManager.isRunning.collectAsState().value,
//                shouldShowResumeButton = !stateHolder.wallbreakerGameManager.isGameOver.collectAsState().value,
//                onResumeButtonPressed = stateHolder.wallbreakerGameManager::resumeGame,
//                onRestartButtonPressed = stateHolder.wallbreakerGameManager::restartGame,
//                areSoundEffectsEnabled = stateHolder.wallbreakerUserPreferencesManager.areSoundEffectsEnabled.collectAsState().value,
//                onSoundEffectsToggled = stateHolder.wallbreakerUserPreferencesManager::onAreSoundEffectsEnabledChanged,
//                isMusicEnabled = stateHolder.wallbreakerUserPreferencesManager.isMusicEnabled.collectAsState().value,
//                onMusicToggled = stateHolder.wallbreakerUserPreferencesManager::onIsMusicEnabledChanged,
//            )
//            GameOverlay(
//                isGameRunning = stateHolder.stateManager.isRunning.collectAsState().value,
//                score = stateHolder.wallbreakerScoreManager.score.collectAsState().value,
//                highScore = stateHolder.wallbreakerScoreManager.highScore.collectAsState().value,
//                onPauseButtonPressed = stateHolder.wallbreakerGameManager::pauseGame,
//            )
        }
    }
}

sealed interface SpaceSquadronGameStateHolder : ExampleStateHolder

fun createSpaceSquadronGameStateHolder(): SpaceSquadronGameStateHolder = SpaceSquadronGameStateHolderImpl()

private class SpaceSquadronGameStateHolderImpl : SpaceSquadronGameStateHolder {
    val stateManager = StateManager.newInstance(shouldAutoStart = false)
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
        PointerInputManager.newInstance(isActiveAboveViewport = true),
    )

    override fun dispose() = kubriko.dispose()
}