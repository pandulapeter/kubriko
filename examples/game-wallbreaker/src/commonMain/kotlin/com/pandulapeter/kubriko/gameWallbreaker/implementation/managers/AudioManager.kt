package com.pandulapeter.kubriko.gameWallbreaker.implementation.managers

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.audioPlayback.AudioPlaybackManager
import com.pandulapeter.kubriko.manager.Manager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kubriko.examples.game_wallbreaker.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
internal class AudioManager(
    private val audioPlaybackManager: AudioPlaybackManager,
    private val userPreferencesManager: UserPreferencesManager
) : Manager() {

    private var hasStartedMusic = false

    override fun onInitialize(kubriko: Kubriko) {
        audioPlaybackManager.preloadSound(
            Res.getUri(PATH_SOUND_EFFECT_CLICK),
            Res.getUri(PATH_SOUND_EFFECT_POP),
        )
        userPreferencesManager.isMusicEnabled.onEach { isMusicEnabled ->
            if (isMusicEnabled) {
                if (hasStartedMusic) {
                    audioPlaybackManager.resumeMusic()
                } else {
                    audioPlaybackManager.playMusic(
                        uri = Res.getUri(PATH_MUSIC),
                        shouldLoop = true,
                    )
                    hasStartedMusic = true
                }
            } else {
                audioPlaybackManager.pauseMusic()
            }
        }.launchIn(scope)
    }

    fun playClickSound() {
        if (userPreferencesManager.areSoundEffectsEnabled.value) {
            audioPlaybackManager.playSound(Res.getUri(PATH_SOUND_EFFECT_CLICK))
        }
    }

    fun playPopSound() {
        if (userPreferencesManager.areSoundEffectsEnabled.value) {
            audioPlaybackManager.playSound(Res.getUri(PATH_SOUND_EFFECT_POP))
        }
    }

    companion object {
        private const val PATH_SOUND_EFFECT_CLICK = "files/sounds/click.wav"
        private const val PATH_SOUND_EFFECT_POP = "files/sounds/pop.wav"
        private const val PATH_MUSIC = "files/music/music.mp3"
    }
}