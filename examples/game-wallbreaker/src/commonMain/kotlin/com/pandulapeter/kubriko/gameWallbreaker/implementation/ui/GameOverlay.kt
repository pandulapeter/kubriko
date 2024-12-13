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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GameOverlay(
    isGameRunning: Boolean,
    score: Int,
    highScore: Int,
    onPauseButtonPressed: () -> Unit,
) {
    val shouldShowScore = remember { mutableStateOf(false) }
    if (score > 0) {
        shouldShowScore.value = true
    }
    AnimatedVisibility(
        visible = isGameRunning,
        enter = fadeIn() + slideIn { IntOffset(0, -it.height) } + scaleIn(),
        exit = scaleOut() + slideOut { IntOffset(0, -it.height) } + fadeOut(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
        ) {
            SmallButton(
                onButtonPressed = onPauseButtonPressed,
                icon = Res.drawable.ic_pause,
                contentDescription = Res.string.pause,
            )
        }
    }
    AnimatedVisibility(
        visible = shouldShowScore.value,
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