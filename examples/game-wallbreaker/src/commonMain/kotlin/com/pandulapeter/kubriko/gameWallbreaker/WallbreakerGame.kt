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
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ScoreManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.WallbreakerGameManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ui.GameOverlay
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ui.PauseMenuOverlay
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
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
    val stateManager = StateManager.newInstance(
        shouldAutoStart = false,
    )
    val scoreManager = ScoreManager()
    val kubriko = Kubriko.newInstance(
        AudioPlaybackManager.newInstance(),
        stateManager,
        ViewportManager.newInstance(
            aspectRatioMode = ViewportManager.AspectRatioMode.Fixed(
                ratio = 1f,
                defaultWidth = 1200.sceneUnit,
            )
        ),
        CollisionManager.newInstance(),
        ShaderManager.newInstance(),
        PersistenceManager.newInstance(fileName = "kubrikoWallbreaker"),
        WallbreakerGameManager(),
        scoreManager,
    )

    override fun dispose() = kubriko.dispose()
}