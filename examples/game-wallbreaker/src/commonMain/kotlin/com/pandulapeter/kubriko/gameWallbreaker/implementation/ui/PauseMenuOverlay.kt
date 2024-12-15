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
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.shared.ui.LargeButton
import com.pandulapeter.kubriko.shared.ui.SmallButton
import kubriko.examples.game_wallbreaker.generated.resources.Res
import kubriko.examples.game_wallbreaker.generated.resources.ic_music_off
import kubriko.examples.game_wallbreaker.generated.resources.ic_music_on
import kubriko.examples.game_wallbreaker.generated.resources.ic_play
import kubriko.examples.game_wallbreaker.generated.resources.ic_sounds_off
import kubriko.examples.game_wallbreaker.generated.resources.ic_sounds_on
import kubriko.examples.game_wallbreaker.generated.resources.img_logo
import kubriko.examples.game_wallbreaker.generated.resources.music_disable
import kubriko.examples.game_wallbreaker.generated.resources.music_enable
import kubriko.examples.game_wallbreaker.generated.resources.new_game
import kubriko.examples.game_wallbreaker.generated.resources.resume
import kubriko.examples.game_wallbreaker.generated.resources.sound_effects_disable
import kubriko.examples.game_wallbreaker.generated.resources.sound_effects_enable
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun PauseMenuOverlay(
    isGameRunning: Boolean,
    shouldShowResumeButton: Boolean,
    onResumeButtonPressed: () -> Unit,
    onRestartButtonPressed: () -> Unit,
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
        shouldShowResumeButton = shouldShowResumeButton,
        onResumeButtonPressed = onResumeButtonPressed,
        onRestartButtonPressed = onRestartButtonPressed,
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
            SmallButton(
                onButtonPressed = onSoundEffectsToggled,
                icon = if (areSoundEffectsEnabled) Res.drawable.ic_sounds_on else Res.drawable.ic_sounds_off,
                contentDescription = if (areSoundEffectsEnabled) Res.string.sound_effects_disable else Res.string.sound_effects_enable,
            )

            SmallButton(
                onButtonPressed = onMusicToggled,
                icon = if (isMusicEnabled) Res.drawable.ic_music_on else Res.drawable.ic_music_off,
                contentDescription = if (isMusicEnabled) Res.string.music_disable else Res.string.music_enable,
            )
        }
    }
}

@Composable
private fun TitleScreen(
    isVisible: Boolean,
    shouldShowResumeButton: Boolean,
    onResumeButtonPressed: () -> Unit,
    onRestartButtonPressed: () -> Unit,
) = AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn() + scaleIn(),
    exit = scaleOut() + fadeOut(),
) {
    val rememberedShouldShowResumeButton = remember { shouldShowResumeButton }
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.img_logo),
                contentDescription = null,
            )
            LargeButton(
                onButtonPressed = if (rememberedShouldShowResumeButton) onResumeButtonPressed else onRestartButtonPressed,
                icon = Res.drawable.ic_play,
                title = if (rememberedShouldShowResumeButton) Res.string.resume else Res.string.new_game,
            )
        }
    }
}