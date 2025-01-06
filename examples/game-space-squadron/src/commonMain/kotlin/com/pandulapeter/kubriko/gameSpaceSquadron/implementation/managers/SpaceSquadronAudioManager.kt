package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.MusicManager
import com.pandulapeter.kubriko.audioPlayback.SoundManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.manager.StateManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kubriko.examples.game_space_squadron.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
internal class SpaceSquadronAudioManager(
    private val stateManager: StateManager,
    private val userPreferencesManager: SpaceSquadronUserPreferencesManager
) : Manager() {
    private val musicManager by manager<MusicManager>()
    private val soundManager by manager<SoundManager>()
    private val soundUrisToPlay = mutableSetOf<String>()

    @OptIn(FlowPreview::class)
    override fun onInitialize(kubriko: Kubriko) {
        combine(
            stateManager.isFocused.debounce(100),
            userPreferencesManager.isMusicEnabled,
        ) { isFocused, isMusicEnabled ->
            isFocused to isMusicEnabled
        }.onEach { (isFocused, isMusicEnabled) ->
            if (isMusicEnabled && isFocused) {
                musicManager.play(
                    uri = Res.getUri(URI_MUSIC),
                    shouldLoop = true,
                )
            } else {
                musicManager.pause(Res.getUri(URI_MUSIC))
            }
        }.launchIn(scope)
    }

    override fun onUpdate(deltaTimeInMilliseconds: Float, gameTimeMilliseconds: Long) {
        soundUrisToPlay.forEach { soundManager.play(Res.getUri(it)) }
        soundUrisToPlay.clear()
    }

    fun playButtonPlaySoundEffect() = playSoundEffect(URI_SOUND_BUTTON_PLAY)

    fun playButtonToggleSoundEffect() = playSoundEffect(URI_SOUND_BUTTON_TOGGLE)

    fun playButtonHoverSoundEffect() = playSoundEffect(URI_SOUND_BUTTON_HOVER)

    fun playShootSoundEffect() = playSoundEffect(URI_SOUND_SHOOT)

    private fun playSoundEffect(uri: String) {
        if (userPreferencesManager.areSoundEffectsEnabled.value) {
            soundUrisToPlay.add(uri)
        }
    }

    companion object {
        private const val URI_MUSIC = "files/music/music.mp3"
        private const val URI_SOUND_BUTTON_PLAY = "files/sounds/button_play.wav"
        private const val URI_SOUND_BUTTON_TOGGLE = "files/sounds/button_toggle.wav"
        private const val URI_SOUND_BUTTON_HOVER = "files/sounds/button_hover.wav"
        private const val URI_SOUND_SHOOT = "files/sounds/shoot.wav"

        fun getMusicUrisToPreload() = listOf(
            URI_MUSIC,
        ).map { Res.getUri(it) }

        fun getSoundUrisToPreload() = listOf(
            URI_SOUND_BUTTON_PLAY,
            URI_SOUND_BUTTON_TOGGLE,
            URI_SOUND_BUTTON_HOVER,
            URI_SOUND_SHOOT,
        ).map { Res.getUri(it) }
    }
}