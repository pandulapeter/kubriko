package com.pandulapeter.kubriko.gameWallbreaker.implementation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kubriko.examples.game_wallbreaker.generated.resources.Res
import kubriko.examples.game_wallbreaker.generated.resources.game_paused
import kubriko.examples.game_wallbreaker.generated.resources.ic_play
import kubriko.examples.game_wallbreaker.generated.resources.img_logo
import kubriko.examples.game_wallbreaker.generated.resources.play
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PauseMenu(
    isGameRunning: Boolean,
    onResumeButtonPressed: () -> Unit,
) {
    AnimatedVisibility(
        visible = !isGameRunning,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(color = Color.Black.copy(0.5f)),
        )
    }
    AnimatedVisibility(
        visible = !isGameRunning,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(Res.drawable.img_logo),
                    contentDescription = null,
                )
                Text(
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    text = stringResource(Res.string.game_paused),
                )
                Button(
                    modifier = Modifier.padding(top = 16.dp),
                    onClick = onResumeButtonPressed,
                    contentPadding = PaddingValues(
                        vertical = 4.dp,
                        horizontal = 16.dp,
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_play),
                            contentDescription = stringResource(Res.string.play),
                        )
                        Text(
                            modifier = Modifier.padding(end = 16.dp),
                            text = stringResource(Res.string.play),
                        )
                    }
                }
            }
        }
    }
}