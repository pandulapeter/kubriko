package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui

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
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.shared.ui.LargeButton
import com.pandulapeter.kubriko.shared.ui.SmallButton
import kubriko.examples.game_space_squadron.generated.resources.Res
import kubriko.examples.game_space_squadron.generated.resources.fullscreen_enter
import kubriko.examples.game_space_squadron.generated.resources.fullscreen_exit
import kubriko.examples.game_space_squadron.generated.resources.ic_fullscreen_enter
import kubriko.examples.game_space_squadron.generated.resources.ic_fullscreen_exit
import kubriko.examples.game_space_squadron.generated.resources.ic_information
import kubriko.examples.game_space_squadron.generated.resources.ic_music_off
import kubriko.examples.game_space_squadron.generated.resources.ic_music_on
import kubriko.examples.game_space_squadron.generated.resources.ic_play
import kubriko.examples.game_space_squadron.generated.resources.ic_sound_effects_off
import kubriko.examples.game_space_squadron.generated.resources.ic_sound_effects_on
import kubriko.examples.game_space_squadron.generated.resources.img_logo
import kubriko.examples.game_space_squadron.generated.resources.information
import kubriko.examples.game_space_squadron.generated.resources.music_disable
import kubriko.examples.game_space_squadron.generated.resources.music_enable
import kubriko.examples.game_space_squadron.generated.resources.new_game
import kubriko.examples.game_space_squadron.generated.resources.sound_effects_disable
import kubriko.examples.game_space_squadron.generated.resources.sound_effects_enable
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun SpaceSquadronPauseMenuOverlay(
    gameAreaModifier: Modifier,
    isGameRunning: Boolean,
    onNewGameButtonPressed: () -> Unit,
    areSoundEffectsEnabled: Boolean,
    onSoundEffectsToggled: () -> Unit,
    isMusicEnabled: Boolean,
    onMusicToggled: () -> Unit,
    isInFullscreenMode: Boolean?,
    onFullscreenModeToggled: () -> Unit,
) = Box(
    modifier = gameAreaModifier,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
                LargeButton(
                    onButtonPressed = onNewGameButtonPressed,
                    icon = Res.drawable.ic_play,
                    title = Res.string.new_game,
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.End)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallButton(
                onButtonPressed = {}, // TODO
                icon = Res.drawable.ic_information,
                contentDescription = Res.string.information,
            )
            SmallButton(
                onButtonPressed = onSoundEffectsToggled,
                icon = if (areSoundEffectsEnabled) Res.drawable.ic_sound_effects_on else Res.drawable.ic_sound_effects_off,
                contentDescription = if (areSoundEffectsEnabled) Res.string.sound_effects_disable else Res.string.sound_effects_enable,
            )
            SmallButton(
                onButtonPressed = onMusicToggled,
                icon = if (isMusicEnabled) Res.drawable.ic_music_on else Res.drawable.ic_music_off,
                contentDescription = if (isMusicEnabled) Res.string.music_disable else Res.string.music_enable,
            )
            isInFullscreenMode?.let {
                SmallButton(
                    onButtonPressed = onFullscreenModeToggled,
                    icon = if (isInFullscreenMode) Res.drawable.ic_fullscreen_exit else Res.drawable.ic_fullscreen_enter,
                    contentDescription = if (isInFullscreenMode) Res.string.fullscreen_exit else Res.string.fullscreen_enter,
                )
            }
        }
    }
}