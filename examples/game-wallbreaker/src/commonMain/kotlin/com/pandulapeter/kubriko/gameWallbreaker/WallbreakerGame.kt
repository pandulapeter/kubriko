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
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ui.PauseButton
import com.pandulapeter.kubriko.gameWallbreaker.implementation.ui.PauseMenu
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
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
            PauseButton(
                isGameRunning = stateHolder.stateManager.isRunning.collectAsState().value,
                onPauseButtonPressed = { stateHolder.stateManager.updateIsRunning(false) },
            )
            PauseMenu(
                isGameRunning = stateHolder.stateManager.isRunning.collectAsState().value,
                onResumeButtonPressed = { stateHolder.stateManager.updateIsRunning(true) },
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
        WallbreakerGameManager()
    )

    override fun dispose() = kubriko.dispose()
}