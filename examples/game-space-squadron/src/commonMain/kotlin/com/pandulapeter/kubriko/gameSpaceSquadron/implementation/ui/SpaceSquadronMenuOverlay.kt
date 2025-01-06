package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.shared.ui.LargeButton
import kubriko.examples.game_space_squadron.generated.resources.Res
import kubriko.examples.game_space_squadron.generated.resources.fullscreen_enter
import kubriko.examples.game_space_squadron.generated.resources.fullscreen_exit
import kubriko.examples.game_space_squadron.generated.resources.ic_fullscreen_enter
import kubriko.examples.game_space_squadron.generated.resources.ic_fullscreen_exit
import kubriko.examples.game_space_squadron.generated.resources.ic_information
import kubriko.examples.game_space_squadron.generated.resources.ic_music_off
import kubriko.examples.game_space_squadron.generated.resources.ic_music_on
import kubriko.examples.game_space_squadron.generated.resources.ic_pause
import kubriko.examples.game_space_squadron.generated.resources.ic_play
import kubriko.examples.game_space_squadron.generated.resources.ic_sound_effects_off
import kubriko.examples.game_space_squadron.generated.resources.ic_sound_effects_on
import kubriko.examples.game_space_squadron.generated.resources.img_logo
import kubriko.examples.game_space_squadron.generated.resources.information
import kubriko.examples.game_space_squadron.generated.resources.music_disable
import kubriko.examples.game_space_squadron.generated.resources.music_enable
import kubriko.examples.game_space_squadron.generated.resources.pause
import kubriko.examples.game_space_squadron.generated.resources.play
import kubriko.examples.game_space_squadron.generated.resources.sound_effects_disable
import kubriko.examples.game_space_squadron.generated.resources.sound_effects_enable
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun SpaceSquadronMenuOverlay(
    modifier: Modifier,
    isGameRunning: Boolean,
    onPlayButtonPressed: () -> Unit,
    onPauseButtonPressed: () -> Unit,
    onInfoButtonPressed: () -> Unit,
    areSoundEffectsEnabled: Boolean,
    onSoundEffectsToggled: () -> Unit,
    isMusicEnabled: Boolean,
    onMusicToggled: () -> Unit,
    isInFullscreenMode: Boolean?,
    onFullscreenModeToggled: () -> Unit,
    onButtonHover: () -> Unit,
) = Box(
    modifier = modifier,
) {
    AnimatedVisibility(
        modifier = Modifier.padding(16.dp),
        visible = isGameRunning,
        enter = fadeIn() + scaleIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        SpaceSquadronIconButton(
            onButtonPressed = onPauseButtonPressed,
            icon = Res.drawable.ic_pause,
            contentDescription = Res.string.pause,
            onPointerEnter = onButtonHover,
        )
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            visible = !isGameRunning,
            enter = fadeIn() + slideIn { IntOffset(0, -it.height) },
            exit = slideOut { IntOffset(0, -it.height) } + fadeOut(),
        ) {
            Title(
                onPlayButtonPressed = onPlayButtonPressed,
                onButtonHover = onButtonHover,
            )
        }
        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.End)
                .padding(16.dp),
            visible = !isGameRunning,
            enter = fadeIn() + slideIn { IntOffset(0, it.height * 8) },
            exit = slideOut { IntOffset(0, it.height * 8) } + fadeOut(),
        ) {
            UserPreferenceControls(
                onInfoButtonPressed = onInfoButtonPressed,
                areSoundEffectsEnabled = areSoundEffectsEnabled,
                onSoundEffectsToggled = onSoundEffectsToggled,
                isMusicEnabled = isMusicEnabled,
                onMusicToggled = onMusicToggled,
                isInFullscreenMode = isInFullscreenMode,
                onFullscreenModeToggled = onFullscreenModeToggled,
                onButtonHover = onButtonHover,
            )
        }
    }
}

@Composable
private fun Title(
    onPlayButtonPressed: () -> Unit,
    onButtonHover: () -> Unit,
) = Box(
    modifier = Modifier.fillMaxWidth(),
) {
    Column(
        modifier = Modifier
            .heightIn(max = 250.dp)
            .align(Alignment.Center)
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier.weight(1f),
            painter = painterResource(Res.drawable.img_logo),
            contentDescription = null,
        )
        SpaceSquadronButton(
            onButtonPressed = onPlayButtonPressed,
            icon = Res.drawable.ic_play,
            title = Res.string.play,
            onPointerEnter = onButtonHover,
        )
    }
}

@Composable
private fun UserPreferenceControls(
    onInfoButtonPressed: () -> Unit,
    areSoundEffectsEnabled: Boolean,
    onSoundEffectsToggled: () -> Unit,
    isMusicEnabled: Boolean,
    onMusicToggled: () -> Unit,
    isInFullscreenMode: Boolean?,
    onFullscreenModeToggled: () -> Unit,
    onButtonHover: () -> Unit,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    SpaceSquadronIconButton(
        onButtonPressed = onInfoButtonPressed,
        icon = Res.drawable.ic_information,
        contentDescription = Res.string.information,
        onPointerEnter = onButtonHover,
    )
    SpaceSquadronIconButton(
        onButtonPressed = onSoundEffectsToggled,
        icon = if (areSoundEffectsEnabled) Res.drawable.ic_sound_effects_on else Res.drawable.ic_sound_effects_off,
        contentDescription = if (areSoundEffectsEnabled) Res.string.sound_effects_disable else Res.string.sound_effects_enable,
        onPointerEnter = onButtonHover,
    )
    SpaceSquadronIconButton(
        onButtonPressed = onMusicToggled,
        icon = if (isMusicEnabled) Res.drawable.ic_music_on else Res.drawable.ic_music_off,
        contentDescription = if (isMusicEnabled) Res.string.music_disable else Res.string.music_enable,
        onPointerEnter = onButtonHover,
    )
    isInFullscreenMode?.let {
        SpaceSquadronIconButton(
            onButtonPressed = onFullscreenModeToggled,
            icon = if (isInFullscreenMode) Res.drawable.ic_fullscreen_exit else Res.drawable.ic_fullscreen_enter,
            contentDescription = if (isInFullscreenMode) Res.string.fullscreen_exit else Res.string.fullscreen_enter,
            onPointerEnter = onButtonHover,
        )
    }
}