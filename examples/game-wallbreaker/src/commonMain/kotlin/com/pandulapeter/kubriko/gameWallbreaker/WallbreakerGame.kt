package com.pandulapeter.kubriko.gameWallbreaker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import com.pandulapeter.kubriko.collision.CollisionManager
import com.pandulapeter.kubriko.gameWallbreaker.implementation.WallbreakerGameManager
import com.pandulapeter.kubriko.implementation.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.StateManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.shader.ShaderManager
import kubriko.examples.game_wallbreaker.generated.resources.Res
import kubriko.examples.game_wallbreaker.generated.resources.game_paused
import kubriko.examples.game_wallbreaker.generated.resources.resume
import org.jetbrains.compose.resources.stringResource

@Composable
fun WallbreakerGame(
    modifier: Modifier = Modifier,
) {
    val stateManager = remember { StateManager.newInstance() }
    val kubriko = remember {
        Kubriko.newInstance(
            stateManager,
            ViewportManager.newInstance(
                aspectRatioMode = ViewportManager.AspectRatioMode.Fixed(
                    ratio = 16f / 9f,
                    defaultWidth = 1200.sceneUnit,
                )
            ),
            CollisionManager.newInstance(),
            ShaderManager.newInstance(),
            WallbreakerGameManager()
        )
    }
    Box {
        KubrikoViewport(
            modifier = modifier.background(Color.Black),
            kubriko = kubriko,
        )
        AnimatedVisibility(
            visible = !stateManager.isRunning.collectAsState().value,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(color = Color.Black.copy(0.5f)),
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        textAlign = TextAlign.Center,
                        text = stringResource(Res.string.game_paused),
                    )
                    Button(
                        onClick = { stateManager.updateIsRunning(true) }
                    ) {
                        Text(
                            text = stringResource(Res.string.resume)
                        )
                    }
                }
            }
        }
    }
}