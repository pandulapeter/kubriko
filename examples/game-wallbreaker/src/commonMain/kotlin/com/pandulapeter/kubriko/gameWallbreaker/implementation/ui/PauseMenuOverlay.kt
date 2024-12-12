package com.pandulapeter.kubriko.gameWallbreaker.implementation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import kubriko.examples.game_wallbreaker.generated.resources.game_paused
import kubriko.examples.game_wallbreaker.generated.resources.ic_music_off
import kubriko.examples.game_wallbreaker.generated.resources.ic_music_on
import kubriko.examples.game_wallbreaker.generated.resources.ic_play
import kubriko.examples.game_wallbreaker.generated.resources.ic_sounds_off
import kubriko.examples.game_wallbreaker.generated.resources.ic_sounds_on
import kubriko.examples.game_wallbreaker.generated.resources.img_logo
import kubriko.examples.game_wallbreaker.generated.resources.music_disable
import kubriko.examples.game_wallbreaker.generated.resources.music_enable
import kubriko.examples.game_wallbreaker.generated.resources.play
import kubriko.examples.game_wallbreaker.generated.resources.sound_effects_disable
import kubriko.examples.game_wallbreaker.generated.resources.sound_effects_enable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PauseMenuOverlay(
    isGameRunning: Boolean,
    onResumeButtonPressed: () -> Unit,
    areSoundEffectsEnabled: Boolean,
    onSoundEffectsToggled: () -> Unit,
    isMusicEnabled: Boolean,
    onMusicToggled: () -> Unit,
) {
    Background(
        isVisible = !isGameRunning,
    )
    UserPreferenceControls(
        isVisible = !isGameRunning,
        areSoundEffectsEnabled = areSoundEffectsEnabled,
        onSoundEffectsToggled = onSoundEffectsToggled,
        isMusicEnabled = isMusicEnabled,
        onMusicToggled = onMusicToggled,
    )
    TitleScreen(
        isVisible = !isGameRunning,
        onResumeButtonPressed = onResumeButtonPressed,
    )
}

@Composable
private fun Background(
    isVisible: Boolean,
) = AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn(),
    exit = fadeOut(),
) {
    Box(
        modifier = Modifier.fillMaxSize().background(color = Color.Black.copy(0.5f)),
    )
}

@Composable
private fun UserPreferenceControls(
    isVisible: Boolean,
    areSoundEffectsEnabled: Boolean,
    onSoundEffectsToggled: () -> Unit,
    isMusicEnabled: Boolean,
    onMusicToggled: () -> Unit,
) = AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn() + slideIn { IntOffset(0, it.height) } + scaleIn(),
    exit = scaleOut() + slideOut { IntOffset(0, it.height) } + fadeOut(),
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        Row(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FloatingActionButton(
                modifier = Modifier.size(40.dp),
                onClick = onSoundEffectsToggled,
            ) {
                Icon(
                    painter = painterResource(if (areSoundEffectsEnabled) Res.drawable.ic_sounds_on else Res.drawable.ic_sounds_off),
                    contentDescription = stringResource(if (areSoundEffectsEnabled) Res.string.sound_effects_disable else Res.string.sound_effects_enable),
                )
            }
            FloatingActionButton(
                modifier = Modifier.size(40.dp),
                onClick = onMusicToggled,
            ) {
                Icon(
                    painter = painterResource(if (isMusicEnabled) Res.drawable.ic_music_on else Res.drawable.ic_music_off),
                    contentDescription = stringResource(if (isMusicEnabled) Res.string.music_disable else Res.string.music_enable),
                )
            }
        }
    }
}

@Composable
private fun TitleScreen(
    isVisible: Boolean,
    onResumeButtonPressed: () -> Unit,
) = AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn() + scaleIn(),
    exit = scaleOut() + fadeOut(),
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
            FloatingActionButton(
                modifier = Modifier.padding(top = 16.dp).height(40.dp),
                onClick = onResumeButtonPressed,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_play),
                        contentDescription = stringResource(Res.string.play),
                    )
                    Text(
                        modifier = Modifier.padding(end = 8.dp),
                        text = stringResource(Res.string.play),
                    )
                }
            }
        }
    }
}