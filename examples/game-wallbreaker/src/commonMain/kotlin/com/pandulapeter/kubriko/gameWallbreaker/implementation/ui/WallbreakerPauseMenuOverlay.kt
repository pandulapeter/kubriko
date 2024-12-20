package com.pandulapeter.kubriko.gameWallbreaker.implementation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.shared.ui.SmallButton
import kubriko.examples.game_wallbreaker.generated.resources.Res
import kubriko.examples.game_wallbreaker.generated.resources.fullscreen_enter
import kubriko.examples.game_wallbreaker.generated.resources.fullscreen_exit
import kubriko.examples.game_wallbreaker.generated.resources.ic_fullscreen_enter
import kubriko.examples.game_wallbreaker.generated.resources.ic_fullscreen_exit
import kubriko.examples.game_wallbreaker.generated.resources.ic_information
import kubriko.examples.game_wallbreaker.generated.resources.ic_music_off
import kubriko.examples.game_wallbreaker.generated.resources.ic_music_on
import kubriko.examples.game_wallbreaker.generated.resources.ic_play
import kubriko.examples.game_wallbreaker.generated.resources.ic_sound_effects_off
import kubriko.examples.game_wallbreaker.generated.resources.ic_sound_effects_on
import kubriko.examples.game_wallbreaker.generated.resources.img_logo
import kubriko.examples.game_wallbreaker.generated.resources.information
import kubriko.examples.game_wallbreaker.generated.resources.music_disable
import kubriko.examples.game_wallbreaker.generated.resources.music_enable
import kubriko.examples.game_wallbreaker.generated.resources.play
import kubriko.examples.game_wallbreaker.generated.resources.sound_effects_disable
import kubriko.examples.game_wallbreaker.generated.resources.sound_effects_enable
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun WallbreakerPauseMenuOverlay(
    gameAreaModifier: Modifier,
    isGameRunning: Boolean,
    shouldShowResumeButton: Boolean,
    onResumeButtonPressed: () -> Unit,
    onRestartButtonPressed: () -> Unit,
    areSoundEffectsEnabled: Boolean,
    onSoundEffectsToggled: () -> Unit,
    isMusicEnabled: Boolean,
    onMusicToggled: () -> Unit,
    isInFullscreenMode: Boolean?,
    onFullscreenModeToggled: () -> Unit,
) = AnimatedVisibility(
    modifier = gameAreaModifier,
    visible = !isGameRunning,
    enter = fadeIn() + scaleIn(),
    exit = scaleOut() + fadeOut(),
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.img_logo),
                contentDescription = null,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val infiniteTransition = rememberInfiniteTransition()
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                SmallButton(
                    modifier = Modifier.scale(scale),
                    onButtonPressed = if (shouldShowResumeButton) onResumeButtonPressed else onRestartButtonPressed,
                    icon = Res.drawable.ic_play,
                    contentDescription = Res.string.play,
                    containerColor = createButtonColor(0f),
                )
                SmallButton(
                    onButtonPressed = {}, // TODO
                    icon = Res.drawable.ic_information,
                    contentDescription = Res.string.information,
                    containerColor = createButtonColor(0.2f),
                )
                SmallButton(
                    onButtonPressed = onSoundEffectsToggled,
                    icon = if (areSoundEffectsEnabled) Res.drawable.ic_sound_effects_on else Res.drawable.ic_sound_effects_off,
                    contentDescription = if (areSoundEffectsEnabled) Res.string.sound_effects_disable else Res.string.sound_effects_enable,
                    containerColor = createButtonColor(0.4f),
                )
                SmallButton(
                    onButtonPressed = onMusicToggled,
                    icon = if (isMusicEnabled) Res.drawable.ic_music_on else Res.drawable.ic_music_off,
                    contentDescription = if (isMusicEnabled) Res.string.music_disable else Res.string.music_enable,
                    containerColor = createButtonColor(0.6f),
                )
                isInFullscreenMode?.let {
                    SmallButton(
                        onButtonPressed = onFullscreenModeToggled,
                        icon = if (isInFullscreenMode) Res.drawable.ic_fullscreen_exit else Res.drawable.ic_fullscreen_enter,
                        contentDescription = if (isInFullscreenMode) Res.string.fullscreen_exit else Res.string.fullscreen_enter,
                        containerColor = createButtonColor(0.8f),
                    )
                }
            }
        }
    }
}