package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui.isSpaceSquadronFontLoaded
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.sprites.SpriteManager
import kubriko.examples.game_space_squadron.generated.resources.Res
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
import kubriko.examples.game_space_squadron.generated.resources.sprite_ship
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.vectorResource

internal class LoadingManager : Manager() {

    private val musicManager by manager<MusicManager>()
    private val soundManager by manager<SoundManager>()
    private val spriteManager by manager<SpriteManager>()
    private val musicUris = AudioManager.getMusicUrisToPreload()
    private val soundUris = AudioManager.getSoundUrisToPreload()
    private val spriteResources = listOf(Res.drawable.sprite_ship)

    override fun onInitialize(kubriko: Kubriko) {
        musicManager.preload(musicUris)
        soundManager.preload(soundUris)
        spriteManager.preload(spriteResources)
    }

    @Composable
    fun isGameLoaded() = isInitialized.collectAsState().value
            && areMenuResourcesLoaded()
            && musicManager.getLoadingProgress(musicUris).collectAsState(0f).value == 1f
            && soundManager.getLoadingProgress(soundUris).collectAsState(0f).value == 1f
            && spriteManager.getLoadingProgress(spriteResources).collectAsState(0f).value == 1f

    @Composable
    private fun areMenuResourcesLoaded() = isSpaceSquadronFontLoaded()
            && vectorResource(Res.drawable.ic_fullscreen_enter).defaultWidth > 1.dp
            && vectorResource(Res.drawable.ic_fullscreen_exit).defaultWidth > 1.dp
            && vectorResource(Res.drawable.ic_information).defaultWidth > 1.dp
            && vectorResource(Res.drawable.ic_music_off).defaultWidth > 1.dp
            && vectorResource(Res.drawable.ic_music_on).defaultWidth > 1.dp
            && vectorResource(Res.drawable.ic_pause).defaultWidth > 1.dp
            && vectorResource(Res.drawable.ic_play).defaultWidth > 1.dp
            && vectorResource(Res.drawable.ic_sound_effects_off).defaultWidth > 1.dp
            && vectorResource(Res.drawable.ic_sound_effects_on).defaultWidth > 1.dp
            && imageResource(Res.drawable.img_logo).width > 1
}