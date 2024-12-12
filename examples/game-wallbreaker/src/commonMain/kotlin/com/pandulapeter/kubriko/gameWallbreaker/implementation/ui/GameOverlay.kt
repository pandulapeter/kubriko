package com.pandulapeter.kubriko.gameWallbreaker.implementation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kubriko.examples.game_wallbreaker.generated.resources.Res
import kubriko.examples.game_wallbreaker.generated.resources.ic_pause
import kubriko.examples.game_wallbreaker.generated.resources.pause
import kubriko.examples.game_wallbreaker.generated.resources.score
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GameOverlay(
    isGameRunning: Boolean,
    score: Int,
    highScore: Int,
    onPauseButtonPressed: () -> Unit,
) {
    AnimatedVisibility(
        visible = isGameRunning,
        enter = fadeIn() + slideIn { IntOffset(0, -it.height) } + scaleIn(),
        exit = scaleOut() + slideOut { IntOffset(0, -it.height) } + fadeOut(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
        ) {
            FloatingActionButton(
                modifier = Modifier.size(40.dp),
                onClick = onPauseButtonPressed,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_pause),
                    contentDescription = stringResource(Res.string.pause),
                )
            }
        }
    }
    AnimatedVisibility(
        visible = score > 0,
        enter = fadeIn() + slideIn { IntOffset(0, -it.height) } + scaleIn(),
        exit = scaleOut() + slideOut { IntOffset(0, -it.height) } + fadeOut(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
        ) {
            Text(
                modifier = Modifier.align(Alignment.TopEnd),
                text = stringResource(Res.string.score, highScore, score),
                color = Color.White,
                textAlign = TextAlign.End,
            )
        }
    }
}